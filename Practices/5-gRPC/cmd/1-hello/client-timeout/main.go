package main

import (
	"bufio"
	"context"
	pb "github.com/KokorinIlya/grpc-test/internal/_1_hello"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/status"
	"io"
	"log"
	"os"
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
	c := pb.NewGreeterClient(conn)
	reader := bufio.NewReader(os.Stdin)

	for {
		name, readErr := reader.ReadString('\n')
		if readErr == io.EOF {
			break
		} else if readErr != nil {
			panic(readErr)
		}
		name = name[:len(name)-1]

		req := &pb.HelloRequest{
			Name: name,
		}
		ctx := context.Background()
		ctx, cancel := context.WithTimeout(ctx, time.Second)
		res, rpcErr := c.SayHello(ctx, req)
		if rpcErr != nil {
			st, ok := status.FromError(rpcErr)
			if ok {
				log.Printf("gRPC returned status error %v", st)
			} else {
				panic(rpcErr)
			}
		} else {
			log.Printf("Greeting: %s", res.GetGreeting())
		}
		cancel()
	}
}
