package main

import (
	amqp "github.com/rabbitmq/amqp091-go"
	"log"
	"os"
	"os/signal"
)

func prepareQueue(ch *amqp.Channel, exchangeName string) <-chan amqp.Delivery {
	q, err := ch.QueueDeclare(
		"",
		false,
		false,
		true,
		false,
		nil,
	)
	if err != nil {
		panic(err)
	}

	err = ch.QueueBind(q.Name, "", exchangeName, false, nil)
	if err != nil {
		panic(err)
	}

	msgs, err := ch.Consume(
		q.Name,
		"",
		false,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		panic(err)
	}
	return msgs
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

	msgs := prepareQueue(ch, "logs_exchange")
	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, os.Interrupt)
	go func() {
		<- sigs
		log.Println("SIGINT received, exiting")
		os.Exit(0)
	}()
	log.Println("Waiting for messages")

	for msg := range msgs {
		ackErr := msg.Ack(false)
		if ackErr != nil {
			panic(ackErr)
		}

		data := string(msg.Body)
		log.Printf("Received message: %v", data)
	}
}
