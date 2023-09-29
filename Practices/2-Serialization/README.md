# Protobuf

## Установка protobuf
* `sudo apt install protobuf-compiler`
* `go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.28`
* `go get google.golang.org/protobuf@v1.28.0`

## Подготовка окружения
* `go mod tidy`

## Компиляция protobuf
* `protoc -I=./proto --go_out=./ <path>/<to>/<proto>.proto`
* Пример: `protoc -I=./proto --go_out=./ ./proto/1-simple/person.proto`

## Запуск
* `go run <path>/<to>/<main>.go`
* Пример: `go run cmd/1-simple/main.go`