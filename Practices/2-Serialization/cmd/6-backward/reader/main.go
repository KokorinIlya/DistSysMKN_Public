package main

import (
	"fmt"
	pb "github.com/KokorinIlya/protobuf-test/internal/_6_backward"
	"google.golang.org/protobuf/proto"
	"os"
)

func main() {
	bytes, err := os.ReadFile("./cmd/6-backward/data.bin")
	if err != nil {
		panic(err)
	}
	var data pb.MyStruct
	err = proto.Unmarshal(bytes, &data)
	if err != nil {
		panic(err)
	}
	fmt.Printf("Unmarshalled data: { %s }\n", data.String())
}
