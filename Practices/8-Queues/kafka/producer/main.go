package main

import (
	"bufio"
	"context"
	"github.com/segmentio/kafka-go"
	"io"
	"log"
	"os"
	"strconv"
	"strings"
)

func main() {
	w := &kafka.Writer{
		Addr:     kafka.TCP("localhost:9092", "localhost:9093"),
		Topic:    "events",
		Balancer: &kafka.Hash{},
	}
	//goland:noinspection GoUnhandledErrorResult
	defer w.Close()

	ctx := context.Background()
	reader := bufio.NewReader(os.Stdin)
	log.Println("Ready to send messages")
	for {
		line, err := reader.ReadString('\n')
		if err == io.EOF {
			break
		} else if err != nil {
			panic(err)
		}
		line = line[:len(line)-1]
		parts := strings.Fields(line)
		if len(parts) != 2 {
			log.Printf("Expected event in <key> <value> format, %v received", line)
			continue
		}
		if _, err = strconv.Atoi(parts[1]); err != nil {
			log.Printf("Cannot convert %v to int: %v", parts[1], err)
			continue
		}
		err = w.WriteMessages(
			ctx,
			kafka.Message{
				Key:   []byte(parts[0]),
				Value: []byte(parts[1]),
			},
		)
		if err != nil {
			log.Fatalf("Error sending message to kafka: %v", err)
		}
	}
}
