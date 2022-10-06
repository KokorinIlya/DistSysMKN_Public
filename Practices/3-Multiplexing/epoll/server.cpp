#include <stdio.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <iostream>
#include <unistd.h>
#include <signal.h>
#include <string>
#include <cstring>
#include <cassert>
#include <vector>
#include <fcntl.h>
#include <unordered_map>
#include <unordered_set>
#include <queue>
#include <unistd.h>
#include <sys/epoll.h>

struct connection_state
{
    std::queue<std::string> ready_responses;
    uint32_t first_response_offset = 0;
    std::string request;
    bool finished_requesting = false;
};

bool should_exit = false;

void sigint_handler_fun(int)
{
    should_exit = true;
}

bool set_nonblocking(int fd)
{
    int flags = fcntl(fd, F_GETFL, 0);
    if (flags == -1) 
    {
        perror("fcntl get flags");
        return false;
    }
    int res = fcntl(fd, F_SETFL, flags | O_NONBLOCK);
    if (res != 0) 
    {
        perror("fcntl set flags");
        return false;
    }
    return true;
}

void close_connection(
    int epoll_fd, int conn, 
    std::unordered_map<int, connection_state>& connections, bool remove_from_map)
{
    std::cout << "Closing " << conn << std::endl;
    assert(epoll_fd >= 0 && conn >= 0 && epoll_fd != conn);
    assert(!remove_from_map || connections.find(conn) != connections.end());
    epoll_ctl(epoll_fd, EPOLL_CTL_DEL, conn, nullptr);
    if (remove_from_map)
    {
        connections.erase(conn);
    }
    close(conn);
}

void set_epoll_events(int epoll_fd, int conn, std::unordered_map<int, connection_state>& connections)
{
    assert(epoll_fd >= 0 && conn >= 0 && epoll_fd != conn && connections.find(conn) != connections.end());
    connection_state& conn_state = connections[conn];

    epoll_event event{};
    event.data.fd = conn;
    event.events = EPOLLET;
    if (!conn_state.finished_requesting)
    {
        event.events |= EPOLLIN;
    }
    if (!conn_state.ready_responses.empty())
    {
        event.events |= EPOLLOUT;
    }

    int res = epoll_ctl(epoll_fd, EPOLL_CTL_MOD, conn, &event);
    if (res == -1)
    {
        perror("set events");
        close_connection(epoll_fd, conn, connections, true);
    }
}

bool start_conn_epoll(int epoll_fd, int conn)
{
    assert(epoll_fd >= 0 && conn >= 0 && epoll_fd != conn);

    epoll_event event{};
    event.data.fd = conn;
    event.events = EPOLLIN | EPOLLET;

    int res = epoll_ctl(epoll_fd, EPOLL_CTL_ADD, conn, &event);
    if (res == -1)
    {
        perror("epoll add");
        return false;
    }
    return true;
}

void do_accept(
    int epoll_fd, int tcp_socket, 
    sockaddr_in* addr_remote_ptr, socklen_t addr_remote_len,
    std::unordered_map<int, connection_state>& connections)
{
    while (true)
    {
        int conn = accept(
            tcp_socket, reinterpret_cast<sockaddr*>(addr_remote_ptr), &addr_remote_len
        );
        if (conn == -1)
        {
            if (errno == EWOULDBLOCK || errno == EAGAIN)
            {
                std::cout << "No more clients to accept" << std::endl;
            }
            else
            {
                perror("accept");
                close_connection(epoll_fd, tcp_socket, connections, false);
            }
            return;
        }
        std::cout << "New connection " << conn << " accepted" << std::endl;
        assert(connections.find(conn) == connections.end());
        if (set_nonblocking(conn) && start_conn_epoll(epoll_fd, conn))
        {
            connections[conn] = connection_state();
        }
        else
        {
            std::cout << "Closing connections" << std::endl;
            close(conn);
        }
    }
}

void do_read(
    int epoll_fd, int conn, 
    std::unordered_map<int, connection_state>& connections, 
    char* buf, uint32_t buffer_len)
{
    assert(epoll_fd >= 0 && conn >= 0 && epoll_fd != conn && connections.find(conn) != connections.end());
    connection_state& conn_state = connections[conn];
    assert(!conn_state.finished_requesting);
    std::cout << "Reading from " << conn << std::endl;

    while (true)
    {
        int bytes_n = read(conn, buf, buffer_len - 1);
        if (bytes_n == 0)
        {
            std::cout << "Client finished transmission" << std::endl;
            conn_state.finished_requesting = true;
            if (conn_state.ready_responses.empty())
            {
                close_connection(epoll_fd, conn, connections, true);
            }
            else
            {
                set_epoll_events(epoll_fd, conn, connections);
            }
            return;
        }
        if (bytes_n == -1)
        {
            if (errno == EWOULDBLOCK || errno == EAGAIN)
            {
                std::cout << "No more data from that socket" << std::endl;
            }
            else
            {
                perror("read");
                close_connection(epoll_fd, conn, connections, true);
            }
            return;
        }
        buf[bytes_n] = '\0';
        std::string received_data(buf);
        std::cout << "Received data " << received_data << std::endl;

        size_t offset = 0;
        while (offset < received_data.length())
        {
            size_t pos = received_data.find("\n", offset);
            if (pos == std::string::npos)
            {
                conn_state.request += received_data.substr(offset);
                break;
            }
            else
            {
                std::string cur_response = "Hello, " + 
                    conn_state.request +
                    received_data.substr(offset, pos - offset + 1);
                conn_state.ready_responses.push(cur_response);
                offset = pos + 1;
                conn_state.request = "";
                if (conn_state.ready_responses.size() == 1)
                {
                    set_epoll_events(epoll_fd, conn, connections);
                }               
            }
        }
    }
}

void do_write(int epoll_fd, int conn, std::unordered_map<int, connection_state>& connections)
{
    assert(epoll_fd >= 0 && conn >= 0 &&epoll_fd != conn && connections.find(conn) != connections.end());
    connection_state& conn_state = connections[conn];
    assert(!conn_state.ready_responses.empty());
    std::cout << "Writing to " << conn << std::endl;

    while (!conn_state.ready_responses.empty())
    {
        std::string const& cur_response = conn_state.ready_responses.front();
        std::cout << "Writing response " << 
            cur_response.c_str() + conn_state.first_response_offset << std::endl;
        while (conn_state.first_response_offset < cur_response.length())
        {
            int write_result = write(
                conn, 
                cur_response.c_str() + conn_state.first_response_offset,
                cur_response.length() - conn_state.first_response_offset
            );
            if (write_result == -1)
            {
                if (errno == EAGAIN || errno == EWOULDBLOCK)
                {
                    std::cout << "Cannot write more data from that socket" << std::endl;
                }
                else
                {
                    perror("write");
                    close_connection(epoll_fd, conn, connections, true);
                }
                return;
            }
            assert(write_result >= 0);
            assert(conn_state.first_response_offset + write_result <= cur_response.length());
            conn_state.first_response_offset += write_result;
        }
        conn_state.ready_responses.pop();
        conn_state.first_response_offset = 0;
    }

    set_epoll_events(epoll_fd, conn, connections);
}

int main()
{
    const uint32_t BUFFER_LEN = 512;
    char buf[BUFFER_LEN];
    const uint16_t PORT = 8765;
    const uint32_t MAX_EVENTS = 100;
    epoll_event events[MAX_EVENTS];

    struct sigaction sigint_handler;
    sigint_handler.sa_handler = sigint_handler_fun;
    sigemptyset(&sigint_handler.sa_mask);
    sigint_handler.sa_flags = 0;
    sigaction(SIGINT, &sigint_handler, NULL);

    int tcp_socket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (tcp_socket == -1)
    {
        perror("Socket creation error");
        return EXIT_FAILURE;
    }

    sockaddr_in addr_local;
    memset(reinterpret_cast<uint8_t*>(&addr_local), 0, sizeof(addr_local));
    addr_local.sin_family = AF_INET;
    addr_local.sin_port = htons(PORT);
    addr_local.sin_addr.s_addr = htonl(INADDR_ANY);

    int bind_result = bind(tcp_socket, reinterpret_cast<sockaddr*>(&addr_local), sizeof(addr_local));
    if(bind_result == -1)
    {
        perror("Socket bind error");
        close(tcp_socket);
        return EXIT_FAILURE;
    }

    int lister_result = listen(tcp_socket, 1);
    if (lister_result == -1)
    {
        perror("Socket listen error");
        close(tcp_socket);
        return EXIT_FAILURE;
    }

    sockaddr_in addr_remote;
    socklen_t addr_remote_len = sizeof(addr_remote);
    memset(reinterpret_cast<uint8_t*>(&addr_remote), 0, addr_remote_len);

    int epoll_fd = epoll_create(1);
    if (epoll_fd < 0)
    {
        perror("epoll");
        close(tcp_socket);
        return EXIT_FAILURE;
    }

    if (!set_nonblocking(tcp_socket) || !start_conn_epoll(epoll_fd, tcp_socket))
    {
        close(tcp_socket);
        return EXIT_FAILURE;
    }
    
    std::unordered_map<int, connection_state> connections;
    while (true)
    {
        if (should_exit)
        {
            std::cout << "Exiting..." << std::endl;
            break;
        }
        std::cout << "Waiting for events" << std::endl;

        int wait_res = epoll_wait(epoll_fd, events, MAX_EVENTS, -1);
        if (wait_res == -1)
        {
            perror("epoll_wait");
            continue;
        }
        for (uint32_t i = 0; i < wait_res; ++i)
        {
            int cur_fd = events[i].data.fd;
            uint32_t cur_events = events[i].events;
            if (cur_events & EPOLLERR)
            {
                close_connection(epoll_fd, cur_fd, connections, cur_fd != tcp_socket);
                continue;
            }
            if (cur_fd == tcp_socket)
            {
                assert(cur_events & EPOLLIN);
                do_accept(epoll_fd, tcp_socket, &addr_remote, addr_remote_len, connections);
            }
            else
            {
                assert(connections.find(cur_fd) != connections.end());
                if (cur_events & EPOLLIN)
                {
                    do_read(epoll_fd, cur_fd, connections, buf, BUFFER_LEN);
                }
                if ((cur_events & EPOLLOUT) && (connections.count(cur_fd) > 0))
                {
                    do_write(epoll_fd, cur_fd, connections);
                }
            }
        }
    }
    
    close_connection(epoll_fd, tcp_socket, connections, false);
    for (auto it = connections.begin(); it != connections.end(); ++it)
    {
        close_connection(epoll_fd, it->first, connections, false);
    }
    close(epoll_fd);
    return EXIT_SUCCESS;
}