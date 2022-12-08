# Kafka

## Установка Kafka
* `wget https://dlcdn.apache.org/kafka/3.2.0/kafka_2.13-3.2.0.tgz`
  * Другие зеркала можно найти на https://kafka.apache.org/downloads
* `tar -xzf kafka_2.13-3.2.0.tgz`

## Запуск нескольких брокеров Kafka
* `cd kafka_2.13-3.2.0`
* `bin/zookeeper-server-start.sh config/zookeeper.properties`
* `cp config/server.properties config/server-1.properties`
* `vim config/server-1.properties`. Меняем следующие строки:
  * `broker.id=1`
  * `listeners=PLAINTEXT://localhost:9092`
  * `log.dirs=/tmp/kafka-logs-1`
* `bin/kafka-server-start.sh config/server-1.properties`
* `cp config/server.properties config/server-2.properties`
* `vim config/server-2.properties`. Меняем следующие строки:
    * `broker.id=2`
    * `listeners=PLAINTEXT://localhost:9093`
    * `log.dirs=/tmp/kafka-logs-2`
* `bin/kafka-server-start.sh config/server-2.properties`

## Операции над топиками
* `bin/kafka-topics.sh --create --topic <name> --replication-factor <R> --partitions <P> --bootstrap-server localhost:9092` — создать реплицированный и партиционированный топик
* `bin/kafka-topics.sh --list --bootstrap-server localhost:9092` — просмотреть список топиков
* `bin/kafka-topics.sh --delete --topic <topic-name> --bootstrap-server localhost:9092` — удалить топик

## Установка зависимостей
* `go get github.com/segmentio/kafka-go`

## Запуск приложения
* `go run path/to/application.go`
* Например: `go run kafka/consumer-group/main.go`
* `go run path/to/application.go arg1 arg2 ...` для передачи аргументов
* Например: `go run kafka/consumer-single/main.go 1`

## Просмотр топиков через `kafkacat`
* `sudo apt install kafkacat`
* `kafkacat -b localhost:9092 -t <topic> -o <offset> -K : -p <partition>`
* Например: `kafkacat -b localhost:9092 -t events -o beginning -K : -p 0`
* Или `kafkacat -b localhost:9092 -t events -o 2 -K : -p 1`