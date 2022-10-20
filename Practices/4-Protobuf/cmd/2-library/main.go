package main

import (
	"fmt"
	pb "github.com/KokorinIlya/protobuf-test/internal/_2_library"
	"google.golang.org/protobuf/proto"
)

func main() {
	babylonLibrary := pb.Library{
		Address: "Babylon",
		Books: []*pb.Book{
			{
				Title:    "In Stahlgewittern",
				Language: "German",
				Author: &pb.Author{
					Name:      "Ernst Jünger",
					BirthDate: &pb.Date{Year: 1895, Month: pb.Month_March, Day: 29},
				},
			},
			{
				Title:    "Опавшие листья",
				Language: "Russian",
				Author: &pb.Author{
					Name:      "Василий Розанов",
					BirthDate: &pb.Date{Year: 1856, Month: pb.Month_May, Day: 2},
				},
			},
			{
				Title:    "Rigodon",
				Language: "French",
				Author: &pb.Author{
					Name:      "Louis-Ferdinand Céline",
					BirthDate: &pb.Date{Year: 1894, Month: pb.Month_May, Day: 27},
				},
			},
		},
	}

	bytes, err := proto.Marshal(&babylonLibrary)
	if err != nil {
		panic(err)
	}
	fmt.Println("Marshalled library: ", bytes)
	fmt.Println("Marshalled library size: ", len(bytes), " bytes")

	var newLibrary pb.Library
	err = proto.Unmarshal(bytes, &newLibrary)
	if err != nil {
		panic(err)
	}
	fmt.Printf("Unmarshalled library: Library { %s }\n", newLibrary.String())
}
