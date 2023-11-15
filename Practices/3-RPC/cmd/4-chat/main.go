package main

import (
	"bufio"
	"context"
	pb "github.com/KokorinIlya/grpc-test/internal/_4_chat"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"io"
	"log"
	"net"
	"os"
)

type server struct {
	pb.UnimplementedChatServer
}

func (*server) StartChat(stream pb.Chat_StartChatServer) error {
	for {
		msg, err := stream.Recv()
		if err == io.EOF {
			log.Printf("No new messages")
			return nil
		} else if err != nil {
			return err
		}
		log.Printf("NEW MESSAGE %v", msg.Text)
	}
}

func main() {
	if len(os.Args) != 2 {
		panic("No port")
	}

	port := os.Args[1]
	socket, err := net.Listen("tcp", ":"+port)
	if err != nil {
		panic(err)
	}
	s := grpc.NewServer()
	pb.RegisterChatServer(s, &server{})
	log.Printf("Listening at %v", socket.Addr())
	go func() {
		err = s.Serve(socket)
		if err != nil {
			panic(err)
		}
	}()

	reader := bufio.NewReader(os.Stdin)
	hostAndPort, err := reader.ReadString('\n')
	if err != nil {
		panic(err)
	}
	hostAndPort = hostAndPort[:len(hostAndPort)-1]
	conn, err := grpc.Dial(
		hostAndPort,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		grpc.WithBlock(),
	)
	if err != nil {
		panic(err)
	}
	//goland:noinspection GoUnhandledErrorResult
	defer conn.Close()

	c := pb.NewChatClient(conn)
	stream, err := c.StartChat(context.Background())
	if err != nil {
		panic(err)
	}
	log.Printf("Ready to send messages")

	for {
		line, err := reader.ReadString('\n')
		if err == io.EOF {
			break
		} else if err != nil {
			panic(err)
		}
		err = stream.Send(&pb.Message{Text: line[:len(line)-1]})
		if err == io.EOF {
			log.Printf("Cannot send message: peer finished reading")
			break
		} else if err != nil {
			panic(err)
		}
	}
	err = stream.CloseSend()
	if err != nil {
		panic(err)
	}
}
