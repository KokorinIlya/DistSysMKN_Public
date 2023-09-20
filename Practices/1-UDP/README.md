# UDP сокеты

## Сборка и запуск сервера
* `g++ server.cpp -o server.out`
* `./server.out`

## Освобождение занятого порта
* `netstat -tulpn | grep 8888`
* `kill -9 <pid>`

## Сборка и запуск клиента
* `g++ client.cpp -o client.out`
* `./client.out`

## Установка и запуск `netcat`-клиента
* `sudo apt install nmap`
* `ncat localhost 8888 -u`
