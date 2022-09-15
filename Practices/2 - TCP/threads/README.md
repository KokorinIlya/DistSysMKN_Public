# Многопоточный сервер

## Сборка и запуск сервера
* `g++ -pthread server.cpp -o server.out`
* `./server.out`

## Установка и запуск `netcat`-клиента
* `sudo apt install nmap`
* `ncat localhost 8765`
