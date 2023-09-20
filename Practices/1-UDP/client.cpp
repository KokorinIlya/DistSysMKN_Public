#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <iostream>
#include <string>
#include <unistd.h>

int main()
{
	const std::string HOST = "127.0.0.1";
	const uint32_t BUFFER_LEN = 512;
	const uint32_t PORT = 8888;
	char buffer[BUFFER_LEN];

	int udp_socket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
	if (udp_socket == -1)
	{
		perror("Socket creation error");
		return EXIT_FAILURE;
	}

	timeval timeout;
	timeout.tv_sec = 1;
	timeout.tv_usec = 0;
	int set_timeout_result = setsockopt(udp_socket, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
	if (set_timeout_result < 0) 
	{
		perror("Socket set timeout error");
		close(udp_socket);
		return EXIT_FAILURE;
	}

	sockaddr_in remote_address;
	socklen_t remote_addr_len = sizeof(remote_address);
	memset(reinterpret_cast<uint8_t*>(&remote_address), 0, remote_addr_len);
	remote_address.sin_family = AF_INET;
	remote_address.sin_port = htons(PORT);
	
	int aton_result = inet_aton(HOST.c_str(), &remote_address.sin_addr);
	if (aton_result == 0) 
	{
		perror("Host address translation error");
		close(udp_socket);
		return EXIT_FAILURE;
	}

	while (true)
	{
		std::cout << "Enter message to send" << std::endl;
		std::string msg;
		std::getline(std::cin, msg);
		if (msg.size() == 0)
		{
			break;
		}

		int send_result = sendto(
			udp_socket, msg.c_str(), msg.size(),
			0, reinterpret_cast<sockaddr*>(&remote_address), remote_addr_len
		);
		if (send_result == -1)
		{
			perror("Socket send error");
			close(udp_socket);
			return EXIT_FAILURE;
		}
	
		int receive_result = recvfrom(
			udp_socket, buffer, BUFFER_LEN - 1,
			0, reinterpret_cast<sockaddr*>(&remote_address), &remote_addr_len
		);
		if (receive_result == -1)
		{
			perror("Socket send error");
			close(udp_socket);
			return EXIT_FAILURE;
		}
		buffer[receive_result] = '\0';
		std::cout << "Received result " << std::string(buffer);
	}

	close(udp_socket);
	return 0;
}