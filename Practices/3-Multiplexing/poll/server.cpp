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
#include <poll.h>

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

void remove_conn(int fd, std::unordered_map<int, connection_state>& connections)
{
    assert(fd >= 0);
    std::cout << "Closing connection " << fd << std::endl;
    auto find_it = connections.find(fd);
    assert(find_it != connections.end());
    connections.erase(find_it);
    close(fd);
}

bool do_read(
    int fd, connection_state& state,
    std::unordered_map<int, connection_state>& connections,
    char* buffer, uint32_t buffer_len)
{
    assert(fd >= 0);
    assert(!state.finished_requesting);

    std::cout << "Reading from " << fd << std::endl;

    int bytes_n = read(fd, buffer, buffer_len - 1);
    if (bytes_n == 0)
    {
        std::cout << "Client finished transmission" << std::endl;
        state.finished_requesting = true;
        if (state.ready_responses.empty())
        {
            remove_conn(fd, connections);
        }
        return true;
    }
    if (bytes_n == -1)
    {
        perror("read");
        remove_conn(fd, connections);
        return false;
    }
    buffer[bytes_n] = '\0';
    std::string received_data(buffer);
    std::cout << "Received data " << received_data << std::endl;

    size_t offset = 0;
    while (offset < received_data.length())
    {
        size_t pos = received_data.find("\n", offset);
        if (pos == std::string::npos)
        {
            state.request += received_data.substr(offset);
            break;
        }
        else
        {
            std::string cur_response = "Hello, " + 
                state.request +
                received_data.substr(offset, pos - offset + 1);
            state.ready_responses.push(cur_response);
            offset = pos + 1;
            state.request = "";
        }
    }
    return true;
}

void do_write(int fd, connection_state& state,
    std::unordered_map<int, connection_state>& connections)
{
    assert(fd >= 0);
    assert(!state.ready_responses.empty());

    std::cout << "Writing to " << fd << std::endl;

    std::string const& cur_response = state.ready_responses.front();
    std::cout << "Writing response " << 
        cur_response.c_str() + state.first_response_offset << std::endl;

    int write_result = write(
        fd, 
        cur_response.c_str() + state.first_response_offset,
        cur_response.length() - state.first_response_offset
    );
    if (write_result == -1)
    {
        perror("write");
        remove_conn(fd, connections);
        return;
    }
    assert(state.first_response_offset + write_result <= cur_response.length());
    if (state.first_response_offset + write_result == cur_response.length())
    {
        state.ready_responses.pop();
        state.first_response_offset = 0;
    }
    else
    {
        state.first_response_offset += write_result;
    }

    if (state.ready_responses.empty() && state.finished_requesting)
    {
        remove_conn(fd, connections);
    }
}

int main()
{
    const uint32_t BUFFER_LEN = 512;
    char buffer[BUFFER_LEN];
    const uint32_t PORT = 8765;

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

    int bind_result = bind(
        tcp_socket, 
        reinterpret_cast<sockaddr*>(&addr_local), sizeof(addr_local)
    );
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

    std::unordered_map<int, connection_state> connections;
    std::cout << "POLLIN = " << POLLIN << ", POLLOUT = " 
                << POLLOUT << ", POLERR = " << POLLERR << ", POLLHUP = " << POLLHUP << std::endl;

    while (true)
    {
        if (should_exit)
        {
            std::cout << "Exiting..." << std::endl;
            break;
        }
        std::cout << "Waiting for incoming requests" << std::endl;

        std::vector<pollfd> fds;
        pollfd main_poll_fd = {tcp_socket, POLLIN, 0};
        fds.push_back(main_poll_fd);

        for (auto it = connections.begin(); it != connections.end(); ++it)
        {
            int fd = it->first;
            connection_state& state = it->second;
            assert(fd >= 0);

            short cur_events = 0;
            if (!state.finished_requesting)
            {
                cur_events |= POLLIN;
            }
            if (!state.ready_responses.empty())
            {
                cur_events |= POLLOUT;
            }

            pollfd poll_fd = {fd, cur_events, 0};
            fds.push_back(poll_fd);
        }

        int poll_res = poll(fds.data(), fds.size(), -1);
        if (poll_res < 0)
        {
            for (auto it = connections.begin(); it != connections.end(); ++it)
            {
                close(it->first);
            }
            return EXIT_FAILURE;
        }

        if (fds[0].revents & POLLIN)
        {
            std::cout << "Accepting new connection" << std::endl;
            int conn = accept(
                tcp_socket, 
                reinterpret_cast<sockaddr*>(&addr_remote), &addr_remote_len
            );
            if (conn == -1)
            {
                perror("Socket accept error");
            }
            else
            {
                std::cout << "New connection " << conn << " accepted" << std::endl;
                assert(connections.find(conn) == connections.end());
                connections[conn] = connection_state();
            }
        }

        for (uint32_t i = 1; i < fds.size(); ++i)
        {
            std::cout << "Events " << fds[i].revents << " from " << fds[i].fd << std::endl;
            bool try_write = true;
            assert(connections.find(fds[i].fd) != connections.end());
            connection_state& state = connections[fds[i].fd];

            short is_read = fds[i].revents & (POLLIN | POLLHUP);
            short is_write = fds[i].revents & POLLOUT;
            if (is_read > 0)
            {
                try_write = do_read(fds[i].fd, state, connections, buffer, BUFFER_LEN);
            }
            if (try_write && is_write > 0)
            {
                do_write(fds[i].fd, state, connections);
            }
        }
    }
    
    close(tcp_socket);
    return EXIT_SUCCESS;
}