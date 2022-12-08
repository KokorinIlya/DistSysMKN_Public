package main

import (
	"bufio"
	"context"
	amqp "github.com/rabbitmq/amqp091-go"
	"io"
	"log"
	"os"
	"strings"
)

func makeExchange(ch *amqp.Channel, exchangeName string) {
	err := ch.ExchangeDeclare(
		exchangeName,
		"topic",
		false,
		false,
		false,
		false,
		nil,
	)
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

	exchangeName := "events_filtered"
	makeExchange(ch, exchangeName)
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
		idx := strings.Index(line, "::")
		if idx == -1 {
			log.Printf("Cannot interpret %v as key-value pair", line)
			continue
		}
		key := line[:idx]
		value := line[idx+2:]

		data := amqp.Publishing{
			ContentType: "text/plain",
			Body:        []byte(value),
		}
		sendErr := ch.PublishWithContext(
			context.Background(),
			exchangeName,
			key,
			false,
			false,
			data,
		)
		if sendErr != nil {
			panic(sendErr)
		}
	}
}
