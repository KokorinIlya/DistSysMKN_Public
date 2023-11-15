package main

import (
	"context"
	pb "github.com/KokorinIlya/grpc-test/internal/_3_server_streaming"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"log"
	"time"
)

func main() {
	conn, err := grpc.Dial(
		"localhost:8081",
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		grpc.WithBlock(),
	)
	if err != nil {
		panic(err)
	}
	//goland:noinspection GoUnhandledErrorResult
	defer conn.Close()
	c := pb.NewEventsHolderClient(conn)
	lastOffset := uint64(0)
	for {
		ctx, cancel := context.WithTimeout(context.Background(), time.Second)
		req := &pb.GetEventsRequest{Offset: lastOffset, Limit: 3}
		res, err := c.GetEvents(ctx, req)
		if err != nil {
			panic(err)
		}
		log.Printf("Received events %v", res.Events)
		lastOffset += uint64(len(res.Events))
		cancel()
		time.Sleep(5 * time.Second)
	}
}
