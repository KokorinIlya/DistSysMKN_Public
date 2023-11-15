package main

import (
	"bufio"
	"context"
	"fmt"
	pb "github.com/KokorinIlya/grpc-test/internal/_3_server_streaming"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"io"
	"log"
	"net"
	"os"
	"sync"
)

var eventsLock = &sync.Mutex{}
var noNewEventsCond = sync.NewCond(eventsLock)
var events []string

type server struct {
	pb.UnimplementedEventsHolderServer
}

func (*server) GetEvents(ctx context.Context, req *pb.GetEventsRequest) (*pb.GetEventsResponse, error) {
	eventsLock.Lock()
	defer eventsLock.Unlock()
	if req.GetOffset() > uint64(len(events)) {
		return nil, status.Error(codes.InvalidArgument, fmt.Sprintf("Max possible offet is %v", len(events)))
	}
	rightBorder := req.Offset + req.Limit
	if rightBorder > uint64(len(events)) {
		rightBorder = uint64(len(events))
	}
	result := &pb.GetEventsResponse{Events: events[req.Offset:rightBorder]}
	return result, nil
}

func (*server) StreamEvents(req *pb.StreamEventsRequest, stream pb.EventsHolder_StreamEventsServer) error {
	log.Printf("Received subscription %v", req)
	eventsLock.Lock()
	if req.Offset > uint64(len(events)) {
		return status.Error(codes.InvalidArgument, fmt.Sprintf("Max possible offet is %v", len(events)))
	}
	eventsLock.Unlock()

	curOffset := req.Offset
	for curOffset-req.Offset < req.Limit {
		eventsLock.Lock()
		for curOffset == uint64(len(events)) {
			noNewEventsCond.Wait()
		}
		eventsToSend := events[curOffset:] // TODO: take limit into account
		eventsLock.Unlock()
		log.Printf("Sending events %v", eventsToSend)
		if len(eventsToSend) == 0 {
			panic("Assertion")
		}
		for _, curEvent := range eventsToSend {
			err := stream.Send(&pb.StreamEventsResponse{Event: curEvent})
			if err != nil {
				return err
			}
		}
		curOffset += uint64(len(eventsToSend))
	}
	return nil
}

func main() {
	socket, err := net.Listen("tcp", ":8081")
	if err != nil {
		panic(err)
	}
	s := grpc.NewServer()
	pb.RegisterEventsHolderServer(s, &server{})
	log.Printf("Listening at %v", socket.Addr())

	go func() {
		err = s.Serve(socket)
		if err != nil {
			panic(err)
		}
	}()

	reader := bufio.NewReader(os.Stdin)
	for {
		event, err := reader.ReadString('\n')
		if err == io.EOF {
			break
		} else if err != nil {
			panic(err)
		}
		event = event[:len(event)-1]
		eventsLock.Lock()
		events = append(events, event)
		noNewEventsCond.Broadcast()
		eventsLock.Unlock()
	}
}
