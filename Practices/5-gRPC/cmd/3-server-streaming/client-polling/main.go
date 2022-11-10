package main

import (
	"context"
	pb "github.com/KokorinIlya/grpc-test/internal/_3_server_streaming"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/status"
	"log"
	"time"
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
		ctx, cancel := context.WithTimeout(context.Background(), time.Second)
		req := &pb.GetEventsRequest{Offset: lastOffset}
		res, rpcErr := c.GetEvents(ctx, req)
		if rpcErr != nil {
			st, ok := status.FromError(rpcErr)
			if ok {
				log.Printf("gRPC returned status error %v", st)
			} else {
				panic(rpcErr)
			}
		} else {
			log.Printf("Received events: %v", res.Events)
			lastOffset += uint64(len(res.Events))
		}
		cancel()
		time.Sleep(3 * time.Second)
	}
}
