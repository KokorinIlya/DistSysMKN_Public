package main

import (
	pb "github.com/KokorinIlya/grpc-test/internal/_2_client_streaming"
	"google.golang.org/grpc"
	"google.golang.org/grpc/peer"
	"io"
	"log"
	"net"
	"strings"
)

type server struct {
	pb.UnimplementedCalculatorServer
}

func (*server) CalculateStatistics(req pb.Calculator_CalculateStatisticsServer) error {
	p, ok := peer.FromContext(req.Context())
	if !ok {
		log.Printf("Received request from unknown")
	} else {
		log.Printf("Received request from %v", p.Addr.String())
	}
	curStat := make(map[string]uint64)
	for {
		curLine, err := req.Recv()
		if err == io.EOF {
			response := &pb.CalculateStatisticsResponse{
				WordCounts: curStat,
			}
			return req.SendAndClose(response)
		} else if err != nil {
			return err
		}
		log.Printf("Received line %v from client", curLine.Line)
		for _, word := range strings.Fields(curLine.Line) {
			word = strings.ToLower(word)
			cnt, ok := curStat[word]
			if !ok {
				cnt = 0
			}
			curStat[word] = cnt + 1
		}
	}
}

func main() {
	socket, err := net.Listen("tcp", ":8081")
	if err != nil {
		panic(err)
	}
	s := grpc.NewServer()
	pb.RegisterCalculatorServer(s, &server{})
	log.Printf("Listening at %v", socket.Addr())
	err = s.Serve(socket)
	if err != nil {
		panic(err)
	}
}
