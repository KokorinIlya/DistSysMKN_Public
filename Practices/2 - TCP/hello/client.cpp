#include <stdio.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <iostream>
#include <unistd.h>
#include <string>
#include <cstring>
#include <cassert>
#include "utils.h"

int main()
{
    const uint32_t BUFFER_LEN = 512;
    const uint32_t PORT = 8765;
    char buf[BUFFER_LEN];

    int tcp_socket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (tcp_socket == -1)
    {
        perror("Socket creation error");
        return EXIT_FAILURE;
    }

    sockaddr_in addr_remote;
    socklen_t addr_remote_len = sizeof(addr_remote);
    memset(reinterpret_cast<uint8_t*>(&addr_remote), 0, addr_remote_len);
    addr_remote.sin_family = AF_INET;
    addr_remote.sin_addr.s_addr = inet_addr("127.0.0.1");
    addr_remote.sin_port = htons(PORT);

    std::cout << "Connecting..." << std::endl;
    int connect_res = connect(tcp_socket, reinterpret_cast<sockaddr*>(&addr_remote), addr_remote_len);
    if (connect_res == -1)
    {
        perror("Socket connect error");
        close(tcp_socket);
        return EXIT_FAILURE;
    }
    std::cout << "Successfully connected" << std::endl;

    while (true)
    {
        std::string line;
        std::getline(std::cin, line);
        if (line.size() == 0)
        {
            break;
        }
        send_data(tcp_socket,  line + "\n", false);

        bool server_active = true;
        while (true)
        {
            int bytes_n = read(tcp_socket, buf, BUFFER_LEN - 1);
            if (bytes_n == 0)
            {
                std::cout << "Server finished transmission" << std::endl;
                server_active = false;
                break;
            }
            if (bytes_n == -1)
            {
                perror("Read error");
                close(tcp_socket);
                return EXIT_FAILURE;
            }
            buf[bytes_n] = '\0';
            printf("%s", buf);
            if (buf[bytes_n - 1] == '\n')
            {
                break;
            }
        }
        if (!server_active)
        {
            break;
        }
    }
    
    std::cout << "Exiting" << std::endl;
    close(tcp_socket);
    return EXIT_SUCCESS;
}