package main

import (
	"fmt"
	pb "github.com/KokorinIlya/protobuf-test/internal/_4_tricks"
	"google.golang.org/protobuf/proto"
)

/**
Return D.C.B.A.Value, or empty string, if some value on the path is no set
*/
func getInnerValue(d *pb.D) string {
	if d == nil {
		return ""
	}
	c := d.C
	if c == nil {
		return ""
	}
	b := c.B
	if b == nil {
		return ""
	}
	a := b.A
	if a == nil {
		return ""
	}
	return a.Value
}

func getInnerValueNPE(d *pb.D) (result string) {
	defer func() {
		err := recover()
		if err != nil {
			result = fmt.Sprintf("Error: %v", err)
		}
	}()
	result = d.C.B.A.Value
	return
}

func innerValue() {
	fmt.Println("-----Retrieving inner value-----")
	d := pb.D{
		C: &pb.C{
			B: &pb.B{
				A: &pb.A{
					Value: "Hello",
				},
			},
		},
	}
	fmt.Println("Naive get(): ", getInnerValue(&d))
	fmt.Println("NPE get(): ", getInnerValueNPE(&d))
	fmt.Println("Advanced get(): ", d.GetC().GetB().GetA().GetValue())
}

func setValue(a *pb.DataHolder, value string) {
	a.Value = value
	a.ValueSet = true
}

func setId(a *pb.DataHolder, id uint32) {
	a.Id = id
	a.IdSet = true
}

func dataHolder() {
	fmt.Println("-----FieldSet-----")
	dh := pb.DataHolder{}
	setValue(&dh, "Hello")
	bytes, err := proto.Marshal(&dh)
	if err != nil {
		panic(err)
	}
	var newDh pb.DataHolder
	err = proto.Unmarshal(bytes, &newDh)
	if err != nil {
		panic(err)
	}

	if newDh.ValueSet {
		fmt.Println("Unmarshalled value: ", newDh.Value)
	} else {
		fmt.Println("No value is set")
	}

	if newDh.IdSet {
		fmt.Println("Unmarshalled id: ", newDh.Value)
	} else {
		fmt.Println("No id is set")
	}
}

func structHolder() {
	fmt.Println("-----Inner structures-----")
	sh := pb.StructHolder{}
	sh.ValueHolder = &pb.StringHolder{
		Value: "Hello",
	}
	bytes, err := proto.Marshal(&sh)
	if err != nil {
		panic(err)
	}
	var newSh pb.StructHolder
	err = proto.Unmarshal(bytes, &newSh)
	if err != nil {
		panic(err)
	}

	if newSh.GetValueHolder() != nil {
		fmt.Println("Unmarshalled value: ", newSh.ValueHolder.Value)
	} else {
		fmt.Println("No value is set")
	}

	if newSh.GetIdHolder() != nil {
		fmt.Println("Unmarshalled id: ", newSh.IdHolder.Value)
	} else {
		fmt.Println("No id is set")
	}
}

func main() {
	innerValue()
	dataHolder()
	structHolder()
}
