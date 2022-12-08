package main

import (
	amqp "github.com/rabbitmq/amqp091-go"
	"log"
	"os"
	"os/signal"
	"strconv"
	"time"
)

func prepareQueue(ch *amqp.Channel, queueName string) <-chan amqp.Delivery {
	q, err := ch.QueueDeclare(
		queueName,
		false,
		false,
		false,
		false,
		nil,
	)
	if err != nil || q.Name != queueName {
		panic(err)
	}

	// Disable for round-robbin push semantics
	err = ch.Qos(
		1,
		0,
		false,
	)
	if err != nil {
		panic(err)
	}

	msgs, err := ch.Consume(
		q.Name,
		"",
		// Set to true for round-robbin push semantics, even when prefetchCount is set to 1
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

	msgs := prepareQueue(ch, "tasks_queue")
	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, os.Interrupt)
	go func() {
		<-sigs
		log.Println("SIGINT received, exiting")
		os.Exit(0)
	}()
	log.Println("Waiting for messages")

	for msg := range msgs {
		data := string(msg.Body)
		log.Printf("Received message: %v", data)
		cnt, convErr := strconv.Atoi(data)
		if convErr != nil || cnt < 0 {
			log.Printf("Cannot convert %v to positive number", data)
		} else {
			time.Sleep(time.Duration(cnt) * time.Second)
			log.Printf("Finished processing %v", data)
		}

		ackErr := msg.Ack(false)
		if ackErr != nil {
			panic(ackErr)
		}
	}
}
