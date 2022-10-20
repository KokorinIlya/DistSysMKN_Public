package main

import (
	"fmt"
	pb "github.com/KokorinIlya/protobuf-test/internal/_7_oneof"
	"google.golang.org/protobuf/proto"
)

func serializeSuccessful() []byte {
	data := pb.Response{
		RequestId: 42,
		ResponseBody: &pb.Response_SuccessfulResponse{
			SuccessfulResponse: "Hello",
		},
	}
	bytes, err := proto.Marshal(&data)
	if err != nil {
		panic(err)
	}
	return bytes
}

func serializeUnsuccessful() []byte {
	data := pb.Response{
		RequestId: 24,
		ResponseBody: &pb.Response_ErrorCode{
			ErrorCode: 404,
		},
	}
	bytes, err := proto.Marshal(&data)
	if err != nil {
		panic(err)
	}
	return bytes
}

func serializeEmpty() []byte {
	data := pb.Response{
		RequestId: 55,
	}
	bytes, err := proto.Marshal(&data)
	if err != nil {
		panic(err)
	}
	return bytes
}

func processResult(bytes []byte) {
	var response pb.Response
	err := proto.Unmarshal(bytes, &response)
	if err != nil {
		panic(err)
	}
	fmt.Printf("Received response: { %s }\n", response.String())
	fmt.Println("Request id: ", response.RequestId)
	body :=  response.GetResponseBody()
	if body == nil {
		fmt.Println("No body received")
		return
	}
	switch body.(type) {
	case *pb.Response_ErrorCode:
		errorCode := body.(*pb.Response_ErrorCode).ErrorCode
		fmt.Println("Error code: ", errorCode)
	case *pb.Response_SuccessfulResponse:
		successfulBody := body.(*pb.Response_SuccessfulResponse).SuccessfulResponse
		fmt.Println("Successful response body: ", successfulBody)
	default:
		panic(fmt.Sprintf("Unknown response body type %v", body))
	}
}

func main() {
	processResult(serializeSuccessful())
	fmt.Println("------")
	processResult(serializeUnsuccessful())
	fmt.Println("------")
	processResult(serializeEmpty())
}
