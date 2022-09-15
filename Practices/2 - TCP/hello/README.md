# TCP сокеты

## Сборка и запуск сервера
* `g++ server.cpp utils.cpp -o server.out`
* `./server.out`

## Освобождение занятого порта
* `netstat -tulpn | grep 8765`
* `kill -9 <pid>`

## Сборка и запуск клиента
* `g++ client.cpp utils.cpp -o client.out`
* `./client.out`

## Установка и запуск `netcat`-клиента
* `sudo apt install nmap`
* `ncat localhost 8765`
