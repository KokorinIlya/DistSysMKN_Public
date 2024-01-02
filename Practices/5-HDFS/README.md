# HDFS

## Установка зависимостей 
* `sudo add-apt-repository ppa:linuxuprising/java`
* `sudo apt update`
* `sudo apt install oracle-java17-jdk oracle-java17-set-default`
* `wget https://dlcdn.apache.org/hadoop/common/hadoop-3.3.3/hadoop-3.3.3.tar.gz`
  * Другие зеркала можно найти на https://hadoop.apache.org/releases.html
* `tar -xzvf hadoop-3.3.3.tar.gz`

## Запуск HDFS в pseudo-distributed режиме
* `cd hadoop-3.3.3/`
* `vim etc/hadoop/hadoop-env.sh`
* Вставьте в файл строку `export JAVA_HOME=/usr/lib/jvm/java-17-oracle/`, изменив путь, если нужно
* Проверьте, что `ssh localhost` завершается без ошибок. Если завершается с ошибкой, то:
  * `ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa`, если нужно
  * `cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys`
  * `chmod 0600 ~/.ssh/authorized_keys`
  * Теперь `ssh localhost` должно завершаться без ошибок
* `vim etc/hadoop/core-site.xml`
* Вставьте следующий текст в файл
```
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://localhost:9000</value>
    </property>
</configuration>
``` 
* `vim etc/hadoop/hdfs-site.xml`
* Вставьте следующий текст в файл, заменив `N` на желаемое значение
```
<configuration>
    <property>
        <name>dfs.replication</name>
        <value>N</value>
    </property>
</configuration>
``` 
* `bin/hdfs namenode -format`
* `sbin/start-dfs.sh`

## Работа с HDFS из командной строки
* `bin/hdfs dfs -<operation> <args...>`
* `bin/hdfs dfs -put /path/to/local/file /path/to/remote/file`
* `bin/hdfs dfs -get /path/to/remote/file /path/to/local/file`
* `bin/hdfs dfs -rm /path/to/file`
* `bin/hdfs dfs -rm -r -f /path/to/directory`
* `bin/hdfs dfs -ls /path/to/directory`
* `bin/hdfs dfs -mkdir /path/to/directory`
* `bin/hdfs dfs -cat /path/to/file`

## Запуск приложения
* `./gradlew run<AppName> --args="<arg1 arg2 arg3>"`
* Например `./gradlew runTree --args="hdfs://localhost:9000 /"`

## Остановка HDFS
* `sbin/stop-dfs.sh`

## Просмотр состояния HDFS через Web UI
* `ssh -L <local port>:localhost:<remote port> <remote ip>`, где:
  * `<local port>` — порт, на котором будет запущен локальный Web UI
  * `<remote port>` — порт, на котором запущен Web UI на удалённой машине
  * `<remote ip>` — IP-адрес удалённой машины, на которой работает HDFS
* Например `ssh -L 54321:localhost:9870 1.2.3.4`
* После этого в браузере можно заходить на http://localhost:54321 и смотреть за состоянием HDFS

## Как узнать remote port?
* С помощью команды `jps` узнаем pid процесса `NameNode`
* `lsof -iTCP -sTCP:LISTEN | grep <NameNode pid>`
* Эквивалентно: выполнить команду `lsof -Pan -iTCP -sTCP:LISTEN -p $(jps | grep "\sNameNode" | cut -d " " -f1)`
* В выводе должны быть две строки:
  * `... TCP 127.0.0.1:9000 (LISTEN)` обозначет порт, на котором NameNode принимает RPC
  * `... TCP *:9870 (LISTEN)` обозначает порт, на котором запущен Web UI

