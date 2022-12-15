# MapReduce

## Запуск HDFS в pseudo-distributed режиме, загрузка данных
* `cd hadoop-3.3.3/`
* `sbin/start-dfs.sh`
* `bin/hdfs dfs -put /path/to/wordcount-dataset/ /`
* `bin/hdfs dfs -put /path/to/average-dataset/ /`

## Установка уровня логгирования
* `export HADOOP_ROOT_LOGGER="LEVEL,console"` для установки уровня логгирования
* Например, `export HADOOP_ROOT_LOGGER="ERROR,console"`
* `unset HADOOP_ROOT_LOGGER` для сброса к значению по умолчанию

## Сборка JAR-архива
* `./gradlew jar`

## Запуск вычисления через Hadoop Streaming
* `bin/hadoop jar share/hadoop/tools/lib/hadoop-streaming-3.3.3.jar -D mapreduce.job.reduces=<number of reducers> -input /path/to/input -output /path/to/output -file /path/to/mapper.py -file /path/to/reducer.py -mapper "python3 mapper.py" -reducer "python3 reducer.py"`
* Например `bin/hadoop jar share/hadoop/tools/lib/hadoop-streaming-3.3.3.jar -D mapreduce.job.reduces=3 -input /wordcount-dataset -output /wc-res -file /home/ubuntu/DistSys_MKN/Practices/9-MapReduce/src/main/python/mapper.py -file /home/ubuntu/DistSys_MKN/Practices/9-MapReduce/src/main/python/reducer.py -mapper "python3 mapper.py" -reducer "python3 reducer.py"`

## Запуск Streaming-скриптов без использования Hadoop для локальной проверки
* `cat /path/to/dataset/* | python3 ./path.to/mapper.py | sort | python3 /path/to/reducer.py`

## Запуск нативного вычисления
* `bin/hadoop jar /path/to/native/jar/9-MapReduce.jar <JobName> <args>`
* Например, `bin/hadoop jar /home/ubuntu/DistSys_MKN/Practices/9-MapReduce/build/libs/9-MapReduce.jar WordCount hdfs://localhost:9000 /wordcount-dataset /wc-res 3`

## Просмотр самых популярных слов
* `bin/hdfs dfs -text /wc-res/* | sort -n -k2,2 | tail -n 10`

## Использование Distributed Cache
* `bin/hadoop jar share/hadoop/tools/lib/hadoop-streaming-3.3.3.jar -D mapreduce.job.reduces=<number of reducers> -input /path/to/input -output /path/to/output -file /path/to/mapper.py -file /path/to/reducer.py -file /path/to/file/1 -file /path/to/file/1 -mapper "python3 <mapper>" -reducer "python3 <reducer>"` для использования с Hadoop Streaming
* Например, `bin/hadoop jar share/hadoop/tools/lib/hadoop-streaming-3.3.3.jar -D mapreduce.job.reduces=3 -input /wordcount-dataset -output /wc-res -file /home/ubuntu/DistSys_MKN/Practices/9-MapReduce/src/main/python/mapper.py -file /home/ubuntu/DistSys_MKN/Practices/9-MapReduce/src/main/python/reducer.py -file /home/ubuntu/DistSys_MKN/Practices/9-MapReduce/stop-words.txt -mapper "python3 mapper.py ./stop-words.txt" -reducer "python3 reducer.py"`
* Перед использованием с нативным вычислением файл нужно загрузить в HDFS: `bin/hdfs dfs -put /path/to/stop-words.txt /`

## Остановка HDFS
* `sbin/stop-dfs.sh`
