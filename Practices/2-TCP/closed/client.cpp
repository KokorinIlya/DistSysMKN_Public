#include <stdio.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <iostream>
#include <unistd.h>
#include <string>
#include <cstring>
#include <signal.h>

void sigpipe_handler_fun(int)
{
    std::cout << "SIGPIPE caught" << std::endl;
}

int main(int argc, char** argv)
{
    if (argc != 2)
    {
        std::cout << "Specify the order of read-write operations" << std::endl;
        return EXIT_FAILURE;
    }

    struct sigaction sigpipe_handler;
    sigpipe_handler.sa_handler = sigpipe_handler_fun;
    sigemptyset(&sigpipe_handler.sa_mask);
    sigpipe_handler.sa_flags = 0;
    sigaction(SIGPIPE, &sigpipe_handler, NULL);

    const uint32_t PORT = 8765;

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

    const uint32_t READ_BUFFER_LEN = 512;
    char read_buf[READ_BUFFER_LEN];
    std::string write_buf = "Hello";
    char* commands = argv[1];
    
    for (uint32_t i = 0; commands[i] != '\0'; ++i)
    {
        if (commands[i] == 'W' || commands[i] == 'w')
        {
            int write_result = write(tcp_socket, write_buf.c_str(), write_buf.length());
            std::cout << "Write result = " << write_result << std::endl;
            if (write_result == -1)
            {
                // ECONNRESET or EPIPE
                std::cout << "Errno is " << errno << std::endl;
                perror("write");
            }
        }
        else if (commands[i] == 'R' || commands[i] == 'r')
        {
            int read_result = read(tcp_socket, read_buf, READ_BUFFER_LEN);
            std::cout << "Read result = " << read_result << std::endl;
        }
        else
        {
            std::cout << "Unknown operation " << commands[i] << std::endl;
            close(tcp_socket);
            return EXIT_FAILURE;
        }
    }
    
    std::cout << "Exiting" << std::endl;
    close(tcp_socket);
    return EXIT_SUCCESS;
}