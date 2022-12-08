package main

import (
	"bufio"
	"context"
	amqp "github.com/rabbitmq/amqp091-go"
	"io"
	"log"
	"os"
)

func makeQueue(ch *amqp.Channel, exchangeName string, queueName string) {
	err := ch.ExchangeDeclare(
		exchangeName,
		"fanout",
		false,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		panic(err)
	}

	queue, err := ch.QueueDeclare(
		queueName,
		false,
		false,
		false,
		false,
		nil,
	)
	if err != nil || queue.Name != queueName {
		panic(err)
	}

	err = ch.QueueBind(queueName, "", exchangeName, false, nil)
	if err != nil {
		panic(err)
	}
}

func main() {
	conn, err := amqp.Dial("amqp://guest:guest@localhost:5672/")
	if err != nil {
		panic(err)
	}
	//goland:noinspection GoUnhandledErrorResult
	defer conn.Close()

	ch, err := conn.Channel()
	if err != nil {
		panic(err)
	}
	//goland:noinspection GoUnhandledErrorResult
	defer ch.Close()

	exchangeName := "tasks_exchange"
	queueName := "tasks_queue"
	makeQueue(ch, exchangeName, queueName)
	log.Println("Ready to send messages")
	reader := bufio.NewReader(os.Stdin)
	for {
		line, readErr := reader.ReadString('\n')
		if readErr == io.EOF {
			break
		} else if readErr != nil {
			panic(readErr)
		}
		line = line[:len(line)-1]

		data := amqp.Publishing{
			ContentType: "text/plain",
			Body:        []byte(line),
		}
		sendErr := ch.PublishWithContext(
			context.Background(),
			exchangeName,
			"",
			false,
			false,
			data,
		)
		if sendErr != nil {
			panic(sendErr)
		}
	}
}
