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
			log.Println("No new messages: chat has been closed by the other side")
			return nil
		} else if err != nil {
			return err
		}
		log.Printf("NEW MESSAGE: %v", msg.Text)
	}
}

func main() {
	if len(os.Args) != 2 {
		panic("Port not specified")
	}

	port := os.Args[1]
	socket, err := net.Listen("tcp", ":" + port)
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
	hostAndPort = hostAndPort[:len(hostAndPort) - 1]
	conn, err := grpc.Dial(
		hostAndPort,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	if err != nil {
		panic(err)
	}
	log.Printf("Connection with %v established", hostAndPort)
	//goland:noinspection GoUnhandledErrorResult
	defer conn.Close()

	c := pb.NewChatClient(conn)
	stream, err := c.StartChat(context.Background())
	if err != nil {
		panic(err)
	}
	log.Println("Ready to send messages")

	for {
		line, readErr := reader.ReadString('\n')
		if readErr == io.EOF {
			break
		} else if readErr != nil {
			log.Printf("Error while reading new message: %v", readErr)
			break
		}
		sendErr := stream.Send(&pb.Message{Text: line[:len(line) - 1]})
		if sendErr == io.EOF {
			log.Println("Cannot send message: channel closed by the other side")
			break
		} else if sendErr != nil {
			log.Printf("Error while reading new message: %v", sendErr)
			break
		}
	}
	err = stream.CloseSend()
	if err != nil {
		panic(err)
	}
}