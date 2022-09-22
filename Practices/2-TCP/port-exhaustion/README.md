# Port Exhaustion

## Упереться в количество открытых файловых дескрипторов
* `go run server/main.go 8081`
* `go run client/main.go 8081`

## Один клиент, один сервер
* `sudo sh -c "ulimit -Hn 100000 && ulimit -Sn 100000 && go run server/main.go 8081"`
* `sudo sh -c "ulimit -Hn 100000 && ulimit -Sn 100000 && go run client/main.go 8081"`

## Получить диапазон эфемерных портов
* `cat /proc/sys/net/ipv4/ip_local_port_range`

## Один клиент, два сервера
* `sudo sh -c "ulimit -Hn 100000 && ulimit -Sn 100000 && go run server/main.go 8081"`
* `sudo sh -c "ulimit -Hn 100000 && ulimit -Sn 100000 && go run server/main.go 8082"`
* `sudo sh -c "ulimit -Hn 100000 && ulimit -Sn 100000 && go run client/main.go 8081 8082"`

## Один клиент, три сервера
* `sudo sh -c "ulimit -Hn 100000 && ulimit -Sn 100000 && go run server/main.go 8081"`
* `sudo sh -c "ulimit -Hn 100000 && ulimit -Sn 100000 && go run server/main.go 8082"`
* `sudo sh -c "ulimit -Hn 100000 && ulimit -Sn 100000 && go run server/main.go 8083"`
* `sudo sh -c "ulimit -Hn 100000 && ulimit -Sn 100000 && go run client/main.go 8081 8082 8083"`

