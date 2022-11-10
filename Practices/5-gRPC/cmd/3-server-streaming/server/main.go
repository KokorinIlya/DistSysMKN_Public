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

func (*server) GetEvents(_ context.Context, req *pb.GetEventsRequest) (*pb.GetEventsResponse, error) {
	eventsLock.Lock()
	defer eventsLock.Unlock()
	if req.Offset > uint64(len(events)) {
		return nil, status.Error(
			codes.InvalidArgument, fmt.Sprintf("Maximal possible offset is %v", len(events)),
		)
	} else {
		result := &pb.GetEventsResponse{
			Events: events[req.Offset:],
		}
		return result, nil
	}
}

func (*server) StreamEvents(req *pb.StreamEventsRequest, stream pb.EventsHolder_StreamEventsServer) error {
	log.Printf("Received request %v", req)

	eventsLock.Lock()
	if req.Offset > uint64(len(events)) {
		eventsLock.Unlock()
		return status.Error(
			codes.InvalidArgument, fmt.Sprintf("Maximal possible offset is %v", len(events)),
		)
	}
	eventsLock.Unlock()

	curOffset := req.Offset
	for curOffset - req.Offset < req.MaxEvents {
		eventsLock.Lock()
		if curOffset == uint64(len(events)) {
			noNewEventsCond.Wait()
		}
		eventsToSend := events[curOffset:]
		eventsLock.Unlock()
		log.Printf("Sending entries: %v", eventsToSend)
		if len(eventsToSend) == 0 {
			panic("Assertion failed!")
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
		event, readErr := reader.ReadString('\n')
		if readErr == io.EOF {
			break
		}
		if readErr != nil {
			panic(readErr)
		}
		event = event[:len(event)-1]
		eventsLock.Lock()
		events = append(events, event)
		noNewEventsCond.Broadcast()
		eventsLock.Unlock()
	}
}
