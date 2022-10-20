package main

import (
	"fmt"
	pb "github.com/KokorinIlya/protobuf-test/internal/_1_simple"
	"google.golang.org/protobuf/proto"
)

func main() {
	person := pb.Person{
		Name: "New user",
		Age:  42,
	}
	bytes, err := proto.Marshal(&person)
	if err != nil {
		panic(err)
	}
	fmt.Println("Marshalled person: ", bytes)

	var newPerson pb.Person
	err = proto.Unmarshal(bytes, &newPerson)
	if err != nil {
		panic(err)
	}
	fmt.Printf("Unmarshalled person: Person { %s }\n", newPerson.String())
}
