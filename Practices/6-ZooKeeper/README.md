# Zookeeper

## Установка зависимостей 
* `sudo apt update`
* `sudo apt install oracle-java17-jdk oracle-java17-set-default`
* `wget https://dlcdn.apache.org/zookeeper/zookeeper-3.7.1/apache-zookeeper-3.7.1-bin.tar.gz`
  * Другие зеркала можно найти на https://zookeeper.apache.org/releases.html
* `tar -xvf apache-zookeeper-3.7.1-bin.tar.gz`

## Запуск Zookeeper в standalone-режиме
* `cd apache-zookeeper-3.7.1-bin`
* `vim conf/zoo.cfg`
* Вставьте следующий текст в файл, изменив `/home/ubuntu/zkdata` на директорию по своему усмотрению
```
tickTime=2000
dataDir=/home/ubuntu/zkdata
clientPort=2181
```
* `bin/zkServer.sh start conf/zoo.cfg` для запуска Zookeeper 
* `bin/zkServer.sh status conf/zoo.cfg` для проверки статуса Zookeeper 
* `bin/zkServer.sh stop conf/zoo.cfg` для остановки Zookeeper 

## Запуск Zookeeper в replicated-режиме
* `cd apache-zookeeper-3.7.1-bin`
* `vim conf/zoo_1.cfg`
* Вставьте следующий текст в файл, изменив `/home/ubuntu/zkdata_1` на директорию по своему усмотрению
```
tickTime=2000
dataDir=/home/ubuntu/zkdata_1
clientPort=2181
initLimit=10
syncLimit=5
server.1=localhost:2888:2889
server.2=localhost:3888:3889
server.3=localhost:4888:4889
```
* `vim conf/zoo_2.cfg`
* Вставьте следующий текст в файл, изменив `/home/ubuntu/zkdata_2` на директорию по своему усмотрению
```
tickTime=2000
dataDir=/home/ubuntu/zkdata_2
clientPort=2182
initLimit=10
syncLimit=5
server.1=localhost:2888:2889
server.2=localhost:3888:3889
server.3=localhost:4888:4889
```
* `vim conf/zoo_3.cfg`
* Вставьте следующий текст в файл, изменив `/home/ubuntu/zkdata_3` на директорию по своему усмотрению
```
tickTime=2000
dataDir=/home/ubuntu/zkdata_3
clientPort=2183
initLimit=10
syncLimit=5
server.1=localhost:2888:2889
server.2=localhost:3888:3889
server.3=localhost:4888:4889
```
* `mkdir -p /home/ubuntu/zkdata_1`
* `mkdir -p /home/ubuntu/zkdata_2`
* `mkdir -p /home/ubuntu/zkdata_3`
* `echo 1 > /home/ubuntu/zkdata_1/myid`
* `echo 2 > /home/ubuntu/zkdata_2/myid`
* `echo 3 > /home/ubuntu/zkdata_3/myid`
* `bin/zkServer.sh start conf/zoo_<n>.cfg` для запуска n-ого узла Zookeeper 
  * Например `bin/zkServer.sh start conf/zoo_1.cfg`
* `bin/zkServer.sh status conf/zoo_<n>.cfg` для проверки статуса n-ого узла Zookeeper 
  * Например `bin/zkServer.sh status conf/zoo_1.cfg`
* `bin/zkServer.sh stop conf/zoo_<n>.cfg` для остановки n-ого узла Zookeeper 
  * Например `bin/zkServer.sh stop conf/zoo_1.cfg`

## Подключение к Zookeeper с помощью CLI
* `cd apache-zookeeper-3.7.1-bin`
* `bin/zkCli.sh -server <host>:<port>`
* Например `bin/zkCli.sh -server localhost:2181`

## Запуск приложения
* `./gradlew run<AppName> --args="<arg1 arg2 arg3>"`
* Например `./gradlew runConnect --args="localhost:2181"` для запуска со standalone zookeeper
* Или `./gradlew runConnect --args="localhost:2181,localhost:2182,localhost:2183"` для запуска с replicated zookeeper

