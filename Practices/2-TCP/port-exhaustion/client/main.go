package main

import (
	"log"
	"net"
	"os"
)

func main() {
	if len(os.Args) < 2 {
		panic("Port list should be provided")
	}
	ports := os.Args[1:]
	var connections []net.Conn
	for {
		port := ports[len(connections)%len(ports)]
		conn, err := net.Dial("tcp", "127.0.0.1:"+port)
		if err != nil {
			log.Printf(
				"Connection error %v after establishing %v connections\n",
				err, len(connections),
			)
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
