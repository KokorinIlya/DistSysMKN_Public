# RabbitMQ

## Установка Docker
* https://docs.docker.com/engine/install/ubuntu/

## Запуск RabbitMQ
* `sudo docker run -it --rm --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.10-management`

## Установка зависимостей
* `go get github.com/rabbitmq/amqp091-go`

## Запуск приложения
* `go run path/to/application.go`
* Например: `go run rabbit/1-tasks/producer/main.go`
* `go run path/to/application.go arg1 arg2 ...` для передачи аргументов
* Например: `go run rabbit/3-filtering/consumer/main.go ERROR.* *.db`

## Просмотр состояния RabbitMQ через WebUI
* http://localhost:15672
* Логин: `guest`
* Пароль: `guest`