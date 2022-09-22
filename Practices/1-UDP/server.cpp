#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <iostream>
#include <unistd.h>
#include <string>
#include <signal.h>

bool should_exit = false;

void sigint_handler_fun(int)
{
    should_exit = true;
}

int main()
{
    struct sigaction sigint_handler;
    sigint_handler.sa_handler = sigint_handler_fun;
    sigemptyset(&sigint_handler.sa_mask);
    sigint_handler.sa_flags = 0;
    sigaction(SIGINT, &sigint_handler, NULL);

    const uint32_t BUFFER_LEN = 512;
    const uint32_t PORT = 8888;
    char buf[BUFFER_LEN];
	
    int udp_socket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (udp_socket == -1)
    {
        perror("Socket creation error");
        return EXIT_FAILURE;
    }

    sockaddr_in addr_local;
    memset(&addr_local, 0, sizeof(addr_local));
    addr_local.sin_family = AF_INET;
    addr_local.sin_port = htons(PORT);
    addr_local.sin_addr.s_addr = htonl(INADDR_ANY);
	
    int bind_result = bind(udp_socket, reinterpret_cast<sockaddr*>(&addr_local), sizeof(addr_local));
    if(bind_result == -1)
    {
        perror("Socket bind error");
        close(udp_socket);
        return EXIT_FAILURE;
    }
	
    sockaddr_in addr_remote;
    socklen_t remove_addr_len = sizeof(addr_remote);
    memset(&addr_remote, 0, remove_addr_len);
    while (true)
    {
        if (should_exit)
        {
            std::cout << "Exiting..." << std::endl;
            break;
        }
        std::cout << "Waiting for incoming requests" << std::endl;
		
        int received_length = recvfrom(
            udp_socket, buf, BUFFER_LEN - 1, 
            0, reinterpret_cast<sockaddr*>(&addr_remote), &remove_addr_len
        );
        if (received_length == -1)
        {
            perror("Socket receive error");
            continue;
        }
        buf[received_length] = '\0';
        
        std::cout << "Received message from " << 
            inet_ntoa(addr_remote.sin_addr) << ":" << ntohs(addr_remote.sin_port) << std::endl;
        std::string received_data(buf);
        std::cout << "Message = " << received_data << std::endl;

        std::string greeting = "Hello, " + received_data;
        int send_result = sendto(
            udp_socket, greeting.c_str(), greeting.size(),
            0, reinterpret_cast<sockaddr*>(&addr_remote), remove_addr_len
        );
        if (send_result == -1)
        {
            perror("Socket send error");
            continue;
        }
    }

    close(udp_socket);
    return EXIT_SUCCESS;
}