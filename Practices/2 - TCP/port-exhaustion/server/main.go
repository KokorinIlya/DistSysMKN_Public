package main

import (
	"log"
	"net"
	"os"
)

func main() {
	if len(os.Args) < 2 {
		panic("Port should be specified")
	}
	port := os.Args[1]
	log.Println("Preparing to listen...")
	socket, err := net.Listen("tcp", ":" + port)
	if err != nil {
		panic(err)
	}
	log.Println("The server is ready to accept connections")
	var connections []net.Conn
	for {
		conn, accErr := socket.Accept()
		if accErr != nil {
			log.Printf("Accept error: %v\n", accErr)
			break
		}
		connections = append(connections, conn)
		if len(connections)%1000 == 0 {
			log.Printf("%v connections established\n", len(connections))
		}
	}
	for _, conn := range connections {
		_ = conn.Close()
	}
}
