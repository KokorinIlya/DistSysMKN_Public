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
			Offset:    lastOffset,
			MaxEvents: 3,
		}
		log.Printf("Connecting with offset %v", req.Offset)
		stream, reqErr := c.StreamEvents(context.Background(), req)
		if reqErr != nil {
			panic(reqErr)
		}
		for {
			curEvent, recvErr := stream.Recv()
			if recvErr == io.EOF {
				break
			} else if recvErr != nil {
				log.Printf("Error while receiving events %v", recvErr)
				break
			}
			log.Printf("Received event %v", curEvent)
			lastOffset++
		}
	}
}
