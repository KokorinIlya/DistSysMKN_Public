package main

import (
	"bufio"
	"context"
	pb "github.com/KokorinIlya/grpc-test/internal/_2_client_streaming"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"io"
	"log"
	"os"
)

func main() {
	conn, err := grpc.Dial(
		"localhost:8081",
		grpc.WithBlock(),
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	if err != nil {
		panic(err)
	}
	//goland:noinspection GoUnhandledErrorResult
	defer conn.Close()
	c := pb.NewCalculatorClient(conn)
	stream, err := c.CalculateStatistics(context.Background())
	if err != nil {
		panic(err)
	}
	reader := bufio.NewReader(os.Stdin)
	for {
		line, err := reader.ReadString('\n')
		if err == io.EOF {
			break
		} else if err != nil {
			panic(err)
		}
		line = line[:len(line)-1]
		err = stream.Send(&pb.Line{Line: line})
		if err != nil {
			panic(err)
		}
	}
	result, err := stream.CloseAndRecv()
	if err != nil {
		panic(err)
	}
	log.Printf("Received %v", result.GetWordCounts())
}
