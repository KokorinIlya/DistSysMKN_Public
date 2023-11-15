# gRPC

## Установка gRPC & Protobuf
* `sudo apt install protobuf-compiler`
* `go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.28`
* `go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@v1.2`
* `go get google.golang.org/protobuf@v1.28.0`
* `go get google.golang.org/grpc@v1.42.0`

## Подготовка окружения
* `go mod tidy`

## Компиляция gRPC & Protobuf
* `protoc -I=./proto --go_out=./ --go-grpc_out=./ ./<path>/<to>/<proto>.proto`
* Пример: `protoc -I=./proto --go_out=./ --go-grpc_out=./ ./proto/1-hello/hello.proto`

## Запуск сервера
* `go run <path>/<to>/<server>.go`
* Пример: `go run cmd/1-hello/server/main.go`

## Запуск клиента
* `go run <path>/<to>/<client>.go`
* Пример: `go run cmd/1-hello/client/main.go`