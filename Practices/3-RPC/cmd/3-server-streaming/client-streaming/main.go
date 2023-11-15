package main

import (
	"context"
	pb "github.com/KokorinIlya/grpc-test/internal/_3_server_streaming"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"io"
	"log"
)

func main() {
	conn, err := grpc.Dial(
		"localhost:8081",
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	if err != nil {
		panic(err)
	}
	//goland:noinspection GoUnhandledErrorResult
	defer conn.Close()
	c := pb.NewEventsHolderClient(conn)

	lastOffset := uint64(0)
	for {
		req := &pb.StreamEventsRequest{
			Offset: lastOffset,
			Limit:  3,
		}
		log.Printf("Connecting with offset %v", req.Offset)
		stream, err := c.StreamEvents(context.Background(), req)
		if err != nil {
			panic(err)
		}
		for {
			curEvent, err := stream.Recv()
			if err == io.EOF {
				break
			} else if err != nil {
				panic(err)
			}
			log.Printf("Received event %v", curEvent)
			lastOffset++
		}
	}
}
