#include "utils.h"
#include <string>
#include <sys/socket.h>
#include <iostream>
#include <unistd.h>

void send_data(int conn, std::string const& data, bool log)
{
    if (log)
    {
        std::cout << "Sending " << data << std::endl;
    }
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
    if (log)
    {
        std::cout << "Finished sending " << data << std::endl;
    }
    return;
}
