package main

import (
	"context"
	"github.com/segmentio/kafka-go"
	"log"
	"os"
	"os/signal"
	"strconv"
	"time"
)

func main() {
	r := kafka.NewReader(kafka.ReaderConfig{
		Brokers:  []string{"localhost:9092", "localhost:9093"},
		GroupID:  "consumer-group-id",
		Topic:    "events",
		MinBytes: 1,
		MaxBytes: 1_000_000,
	})
	//goland:noinspection GoUnhandledErrorResult
	defer r.Close()

	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, os.Interrupt)
	go func() {
		<-sigs
		log.Println("SIGINT")
		os.Exit(0)
	}()

	ctx := context.Background()
	log.Println("Ready to consume")
	for {
		msg, err := r.FetchMessage(ctx)
		if err != nil {
			break
		}

		xStr := string(msg.Value)
		log.Printf(
			"Consuming { key = %v, value = %v, offset = %v } from partition %v",
			string(msg.Key), xStr, msg.Offset, msg.Partition,
		)
		x, err := strconv.Atoi(xStr)
		if err != nil || x < 0 {
			log.Printf("Cannot get positive int from %v, skipping", xStr)
		} else {
			time.Sleep(time.Duration(x) * time.Second)
		}

		log.Printf("Commiting message")
		err = r.CommitMessages(ctx, msg)
		if err != nil {
			panic(err)
		}
	}

	if err := r.Close(); err != nil {
		log.Fatal("failed to close reader:", err)
	}
}
