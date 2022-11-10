package main

import (
	"bufio"
	"context"
	pb "github.com/KokorinIlya/grpc-test/internal/_2_client_streaming"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/status"
	"io"
	"log"
	"os"
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
	c := pb.NewCalculatorClient(conn)
	stream, err := c.CalculateStatistics(context.Background())
	if err != nil {
		st, ok := status.FromError(err)
		if ok {
			log.Printf("gRPC returned status error %v", st)
			return
		} else {
			panic(err)
		}
	}

	reader := bufio.NewReader(os.Stdin)
	for {
		line, readErr := reader.ReadString('\n')
		if readErr == io.EOF {
			break
		} else if readErr != nil {
			panic(readErr)
		}
		sendErr := stream.Send(&pb.Line{Line: line})
		if sendErr != nil {
			panic(sendErr)
		}
	}
	result, err := stream.CloseAndRecv()
	if err != nil {
		panic(err)
	}
	log.Printf("Received result %v", result.WordCounts)
}
