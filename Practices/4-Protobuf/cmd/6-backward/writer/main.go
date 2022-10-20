package main

import (
	pb "github.com/KokorinIlya/protobuf-test/internal/_6_backward"
	"google.golang.org/protobuf/proto"
	"os"
)

func main() {
	data := pb.MyStruct{
		Inner: []*pb.InnerStruct{
			{
				A: "a_1",
				C: "c_1",
			},
			{
				B: "b_2",
				C: "c_2",
			},
		},
	}
	bytes, err := proto.Marshal(&data)
	if err != nil {
		panic(err)
	}
	err = os.WriteFile("./cmd/6-backward/data.bin", bytes, 0666)
	if err != nil {
		panic(err)
	}
}
