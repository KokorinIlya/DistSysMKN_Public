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
        perror("fcntl get");
        return false;
    }
    int res = fcntl(fd, F_SETFL, flags | O_NONBLOCK);
    if (res != 0) 
    {
        perror("fcntl set");
        return false;
    }
    return true;
}

void remove_conns(
    std::unordered_map<int, connection_state>& connections,
    std::unordered_set<int> const& conns_to_remove)
{
    for (auto it = conns_to_remove.begin(); it != conns_to_remove.end(); ++it)
    {
        std::cout << "Closing connection " << *it << std::endl;
        auto find_it = connections.find(*it);
        assert(find_it != connections.end());
        connections.erase(find_it);
        close(*it);
    }
}

void do_reads(
    std::unordered_map<int, connection_state>& connections, 
    char* buf, uint32_t buffer_len)
{
    std::unordered_set<int> conns_to_remove;
    for (auto it = connections.begin(); it != connections.end(); ++it)
    {
        int conn = it->first;
        connection_state& cur_state = it->second;
        if (cur_state.finished_requesting)
        {
            continue;
        }

        std::cout << "Reading from " << conn << std::endl;

        while (true)
        {
            int bytes_n = read(conn, buf, buffer_len - 1);
            if (bytes_n == 0)
            {
                std::cout << "Client finished transmission" << std::endl;
                cur_state.finished_requesting = true;
                break;
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
                    conns_to_remove.insert(conn);
                }
                errno = 0;
                break;
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
                    cur_state.request += received_data.substr(offset);
                    break;
                }
                else
                {
                    std::string cur_response = "Hello, " + 
                        cur_state.request +
                        received_data.substr(offset, pos - offset + 1);
                    cur_state.ready_responses.push(cur_response);
                    offset = pos + 1;
                    cur_state.request = "";
                }
            }
        }
    }
    remove_conns(connections, conns_to_remove);
}

void do_writes(std::unordered_map<int, connection_state>& connections)
{
    std::unordered_set<int> conns_to_remove;
    for (auto it = connections.begin(); it != connections.end(); ++it)
    {
        int conn = it->first;
        connection_state& cur_state = it->second;
        std::cout << "Writing to " << conn << std::endl;
        bool stop = false; 

        while (!cur_state.ready_responses.empty())
        {
            std::string const& cur_response = cur_state.ready_responses.front();
            std::cout << "Writing response " << 
                cur_response.c_str() + cur_state.first_response_offset << std::endl;
            while (true)
            {
                int write_result = write(
                    conn, 
                    cur_response.c_str() + cur_state.first_response_offset,
                    cur_response.length() - cur_state.first_response_offset
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
                        conns_to_remove.insert(conn);
                    }
                    errno = 0;
                    stop = true;
                    break;
                }
                assert(cur_state.first_response_offset + write_result <= cur_response.length());
                if (cur_state.first_response_offset + write_result == cur_response.length())
                {
                    cur_state.ready_responses.pop();
                    cur_state.first_response_offset = 0;
                    break;
                }
                else
                {
                    cur_state.first_response_offset += write_result;
                }
            }
            if (stop)
            {
                break;
            }
        }

        if (cur_state.ready_responses.empty() && cur_state.finished_requesting)
        {
            conns_to_remove.insert(conn);
        }
    }
    remove_conns(connections, conns_to_remove);
}

int main()
{
    const uint32_t BUFFER_LEN = 512;
    const uint32_t PORT = 8765;
    char buf[BUFFER_LEN];

    struct sigaction sigint_handler;
    sigint_handler.sa_handler = sigint_handler_fun;
    sigemptyset(&sigint_handler.sa_mask);
    sigint_handler.sa_flags = 0;
    sigaction(SIGINT, &sigint_handler, NULL);

    int tcp_socket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (tcp_socket == -1)
    {
        perror("socket");
        return EXIT_FAILURE;
    }

    sockaddr_in addr_local;
    memset(&addr_local, 0, sizeof(addr_local));
    addr_local.sin_family = AF_INET;
    addr_local.sin_port = htons(PORT);
    addr_local.sin_addr.s_addr = htonl(INADDR_ANY);

    int bind_result = bind(tcp_socket, reinterpret_cast<sockaddr*>(&addr_local), sizeof(addr_local));
    if(bind_result == -1)
    {
        perror("bind");
        close(tcp_socket);
        return EXIT_FAILURE;
    }

    int lister_result = listen(tcp_socket, 1);
    if (lister_result == -1)
    {
        perror("listen");
        close(tcp_socket);
        return EXIT_FAILURE;
    }

    sockaddr_in addr_remote;
    socklen_t addr_remote_len = sizeof(addr_remote);
    memset(&addr_remote, 0, addr_remote_len);

    if (!set_nonblocking(tcp_socket))
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
        std::cout << "Waiting for incoming requests" << std::endl;

        int conn = accept(tcp_socket, reinterpret_cast<sockaddr*>(&addr_remote), &addr_remote_len);
        if (conn == -1)
        {
            if (errno == EAGAIN || errno == EWOULDBLOCK)
            {
                std::cout << "No client now" << std::endl;
            }
            else
            {
                perror("accept");
            }
            errno = 0;
        }
        else
        {
            std::cout << "New connection " << conn << " accepted" << std::endl;
            assert(connections.find(conn) == connections.end());
            if (set_nonblocking(conn))
            {
                connections[conn] = connection_state();
            }
            else
            {
                std::cout << "Closing connections" << std::endl;
                close(conn);
            }
        }
        do_reads(connections, buf, BUFFER_LEN);
        do_writes(connections);

        sleep(5);
    }
    
    close(tcp_socket);
    return EXIT_SUCCESS;
}