package main

import (
	"context"
	"fmt"
	pb "github.com/KokorinIlya/grpc-test/internal/_1_hello"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/peer"
	"google.golang.org/grpc/status"
	"log"
	"net"
	"time"
)

type server struct {
	pb.UnimplementedGreeterServer
}

func (*server) SayHello(ctx context.Context,
	req *pb.HelloRequest) (*pb.HelloResponse, error) {
	p, ok := peer.FromContext(ctx)
	if !ok {
		log.Printf("Received request from unknown")
	} else {
		log.Printf("Received request from %v", p.Addr.String())
	}

	if req.GetName() == "Hitler" {
		return nil, status.Error(codes.PermissionDenied,
			"Hitler is not welcome here")
	}
	if req.GetName() == "Sloth" {
		time.Sleep(time.Minute)
	}
	if req.GetName() == "Wait" {
		cancel := <-ctx.Done()
		log.Printf("Cancel is %v", cancel)
		return nil, status.Error(codes.Canceled, "Request cancelled")
	}

	return &pb.HelloResponse{
		Greeting: fmt.Sprintf("Hello, %v", req.GetName()),
	}, nil
}

func main() {
	socket, err := net.Listen("tcp", ":8081")
	if err != nil {
		panic(err)
	}
	s := grpc.NewServer()
	pb.RegisterGreeterServer(s, &server{})
	log.Printf("Listening at %v", socket.Addr())
	err = s.Serve(socket)
	if err != nil {
		panic(err)
	}
}
