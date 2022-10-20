package main

import (
	"fmt"
	pb "github.com/KokorinIlya/protobuf-test/internal/_5_packing"
	"google.golang.org/protobuf/proto"
)

func packed() {
	x := pb.PackedIntHolder{
		Ids: []uint32{1, 3, 3, 7, 5, 2000},
	}
	bytes, err := proto.Marshal(&x)
	if err != nil {
		panic(err)
	}
	fmt.Println("Marshalled packed struct: ", bytes)
}

func unpacked() {
	x := pb.UnpackedIntHolder{
		Ids: []uint32{1, 3, 3, 7, 5, 2000},
	}
	bytes, err := proto.Marshal(&x)
	if err != nil {
		panic(err)
	}
	fmt.Println("Marshalled unpacked struct: ", bytes)
}

func main() {
	packed()
	unpacked()
}
