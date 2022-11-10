package main

import (
	"context"
	"fmt"
	pb "github.com/KokorinIlya/grpc-test/internal/_1_hello"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/peer"
	"google.golang.org/grpc/status"
	"log"
	"net"
	"time"
)

type server struct {
	pb.UnimplementedGreeterServer
}

func (*server) SayHello(ctx context.Context, req *pb.HelloRequest) (*pb.HelloResponse, error) {
	p, ok := peer.FromContext(ctx)
	if !ok {
		log.Printf("Received request %v from unknown user", req)
	} else {
		log.Printf("Received request %v from %v", req, p.Addr.String())
	}
	md, ok := metadata.FromIncomingContext(ctx)
	if ok {
		token := md.Get("token")
		log.Printf("Token is %v", token)
	} else {
		log.Println("Cannot get request metadata")
	}
	<-ctx.Done()
	log.Println("Request finished")

	if req.Name == "Hitler" {
		return nil, status.Error(codes.PermissionDenied, "Hitler is not welcome here")
	}
	if req.Name == "Sloth" {
		time.Sleep(2 * time.Second)
	}
	return &pb.HelloResponse{
		Greeting: fmt.Sprintf("Hello, %v", req.Name),
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
