#include <stdio.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <iostream>
#include <unistd.h>
#include <string>
#include <signal.h>
#include <cstring>

bool should_exit = false;

void sigint_handler_fun(int)
{
    should_exit = true;
}

int main()
{
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
            perror("Socket accept error");
            continue;
        }
        std::cout << "Connected client " << 
            inet_ntoa(addr_remote.sin_addr) << ":" << ntohs(addr_remote.sin_port) << std::endl;
        close(conn);
    }
    
    close(tcp_socket);
    return EXIT_SUCCESS;
}