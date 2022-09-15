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
#include <thread>
#include <vector>

bool should_exit = false;

void sigint_handler_fun(int)
{
    should_exit = true;
}

void send_data(int conn, std::string const& data)
{
    std::cout << "Sending " << data << std::endl;
    uint32_t offset = 0;
    while (offset < data.length())
    {
        int write_result = write(conn, data.c_str() + offset, data.length() - offset);
        if (write_result == -1)
        {
            perror("Connection write error");
            return;
        }
        offset += write_result;
    }
    std::cout << "Finished sending " << data << std::endl;
    return;
}

void process_client(int conn, int buffer_len)
{
    char* buf = static_cast<char*>(malloc(buffer_len));
    std::string response = "Hello, ";

    while (true)
    {
        int bytes_n = read(conn, buf, buffer_len - 1);
        if (bytes_n == 0)
        {
            std::cout << "Client finished transmission" << std::endl;
            break;
        }
        if (bytes_n == -1)
        {
            perror("Read error");
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
                response += received_data.substr(offset);
                break;
            }
            else
            {
                response += received_data.substr(offset, pos - offset + 1);
                send_data(conn, response);
                offset = pos + 1;
                response = "Hello, ";
            }
        }
    }

    free(buf);
    close(conn);
}

int main()
{
    const uint32_t BUFFER_LEN = 512;
    const uint32_t PORT = 8765;

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
    memset(&addr_local, 0, sizeof(addr_local));
    addr_local.sin_family = AF_INET;
    addr_local.sin_port = htons(PORT);
    addr_local.sin_addr.s_addr = htonl(INADDR_ANY);

    int bind_result = bind(
        tcp_socket, 
        reinterpret_cast<sockaddr*>(&addr_local),
        sizeof(addr_local)
    );
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
    memset(&addr_remote, 0, addr_remote_len);
    std::vector<std::thread> threads;

    while (true)
    {
        if (should_exit)
        {
            std::cout << "Exiting..." << std::endl;
            break;
        }
        std::cout << "Waiting for incoming requests" << std::endl;

        int conn = accept(
            tcp_socket, 
            reinterpret_cast<sockaddr*>(&addr_remote),
            &addr_remote_len
        );
        if (conn == -1)
        {
            perror("Socket accept error");
            continue;
        }
        std::cout << "Connected client " << 
            inet_ntoa(addr_remote.sin_addr) << ":" << 
            ntohs(addr_remote.sin_port) << std::endl;
        threads.push_back(
            std::thread(process_client, conn, BUFFER_LEN)
        );
    }
    
    close(tcp_socket);
    for (std::thread& t : threads) 
    {
        t.join();
    }
    return EXIT_SUCCESS;
}