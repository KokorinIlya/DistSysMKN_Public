package main

import (
	"fmt"
	pb "github.com/KokorinIlya/protobuf-test/internal/_3_missing"
	"google.golang.org/protobuf/proto"
)

func serde(person *pb.Person, printBytes bool) *pb.Person {
	bytes, err := proto.Marshal(person)
	if err != nil {
		panic(err)
	}
	if printBytes {
		fmt.Println("Marshalled person: ", bytes)
	}

	var newPerson pb.Person
	err = proto.Unmarshal(bytes, &newPerson)
	if err != nil {
		panic(err)
	}
	fmt.Printf("Unmarshalled person: Person { %s }\n", newPerson.String())
	return &newPerson
}

func missingInt() {
	fmt.Println("-----Missing int field-----")
	person := pb.Person{}
	person.Name = "Имя"
	person.Address = &pb.Address{
		Country: "Россия",
		City:    "Санкт-Петербург",
	}
	person.Role = pb.Role_USER
	person.Phones = []string{"8-800-555-35-35", "42"}
	newPerson := serde(&person, true)
	fmt.Printf("Unmarshalled age: %v\n", newPerson.Age)
}

func zeroInt() {
	fmt.Println("-----Zero int field-----")
	person := pb.Person{}
	person.Name = "Имя"
	person.Address = &pb.Address{
		Country: "Россия",
		City:    "Санкт-Петербург",
	}
	person.Age = 0 // Сериализация в ноль лет
	person.Role = pb.Role_USER
	person.Phones = []string{"8-800-555-35-35", "42"}
	newPerson := serde(&person, true)
	fmt.Printf("Unmarshalled age: %v\n", newPerson.Age)
}

func missingString() {
	fmt.Println("-----Missing string field-----")
	person := pb.Person{}
	person.Age = 42
	person.Address = &pb.Address{
		Country: "Россия",
		City:    "Санкт-Петербург",
	}
	person.Role = pb.Role_USER
	person.Phones = []string{"8-800-555-35-35", "42"}
	newPerson := serde(&person, false)
	fmt.Printf("Unmarshalled name: %v\n", newPerson.Name)
}

func missingEnum() {
	fmt.Println("-----Missing enum field-----")
	person := pb.Person{}
	person.Name = "Имя"
	person.Age = 42
	person.Address = &pb.Address{
		Country: "Россия",
		City:    "Санкт-Петербург",
	}
	person.Phones = []string{"8-800-555-35-35", "42"}
	newPerson := serde(&person, false)
	fmt.Printf("Unmarshalled role: %v\n", newPerson.Role)
}

func missingRepeated() {
	fmt.Println("-----Missing repeated field-----")
	person := pb.Person{}
	person.Name = "Имя"
	person.Age = 42
	person.Address = &pb.Address{
		Country: "Россия",
		City:    "Санкт-Петербург",
	}
	person.Role = pb.Role_USER
	newPerson := serde(&person, false)
	fmt.Printf("Unmarshalled phones: %v\n", newPerson.Phones)
}

func missingStruct() {
	fmt.Println("-----Missing struct field-----")
	person := pb.Person{}
	person.Name = "Имя"
	person.Age = 42
	person.Phones = []string{"8-800-555-35-35", "42"}
	person.Role = pb.Role_USER
	newPerson := serde(&person, false)
	fmt.Printf("Unmarshalled address: %v\n", newPerson.Address)
}

func missingEverything() {
	fmt.Println("-----Missing all fields-----")
	person := pb.Person{}
	newPerson := serde(&person, true)
	fmt.Printf("Unmarshalled age: %v\n", newPerson.Age)
	fmt.Printf("Unmarshalled name: %v\n", newPerson.Name)
	fmt.Printf("Unmarshalled role: %v\n", newPerson.Role)
	fmt.Printf("Unmarshalled phones: %v\n", newPerson.Phones)
	fmt.Printf("Unmarshalled address: %v\n", newPerson.Address)
}

func defaultEverything() {
	fmt.Println("-----Default all fields-----")
	person := pb.Person{}
	person.Age = 0
	person.Name = ""
	person.Role = pb.Role_UNDEFINED
	person.Phones = []string{}
	person.Address = nil
	newPerson := serde(&person, true)
	fmt.Printf("Unmarshalled age: %v\n", newPerson.Age)
	fmt.Printf("Unmarshalled name: %v\n", newPerson.Name)
	fmt.Printf("Unmarshalled role: %v\n", newPerson.Role)
	fmt.Printf("Unmarshalled phones: %v\n", newPerson.Phones)
	fmt.Printf("Unmarshalled address: %v\n", newPerson.Address)
}

func emptyStructIsNotNil() {
	fmt.Println("-----Empty struct is not nil-----")
	person := pb.Person{}
	person.Address = &pb.Address{}
	newPerson := serde(&person, true)
	fmt.Printf("Unmarshalled address: %v\n", newPerson.Address)
}

func main() {
	missingInt()
	zeroInt()
	missingString()
	missingEnum()
	missingRepeated()
	missingStruct()
	missingEverything()
	defaultEverything()
	emptyStructIsNotNil()
}
