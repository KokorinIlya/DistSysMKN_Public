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
	conn, err := grpc.Dial("localhost:8081",
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		grpc.WithBlock(),
	)
	if err != nil {
		panic(err)
	}
	//goland:noinspection GoUnhandledErrorResult
	defer conn.Close()
	c := pb.NewGreeterClient(conn)
	reader := bufio.NewReader(os.Stdin)

	for {
		name, err := reader.ReadString('\n')
		if err == io.EOF {
			break
		} else if err != nil {
			panic(err)
		}
		name = name[:len(name)-1]
		req := &pb.HelloRequest{
			Name: name,
		}
		ctx := context.Background()
		ctx, cancel := context.WithTimeout(ctx, time.Second)

		res, err := c.SayHello(ctx, req) // Goroutine blocks until response is received
		if err != nil {
			st, ok := status.FromError(err)
			if !ok {
				panic(err)
			} else {
				log.Printf("Error is %v:%v", st.Code(), st.Message())
			}
		} else {
			log.Printf("Result is %v", res.GetGreeting())
		}
		cancel()
	}
}
