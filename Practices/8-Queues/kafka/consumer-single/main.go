package main

import (
	"context"
	"fmt"
	"github.com/segmentio/kafka-go"
	"io"
	"io/ioutil"
	"log"
	"os"
	"sort"
	"strconv"
	"strings"
)

type state struct {
	balance   map[string]int
	path      string
	dirNumber int
	maxSize   uint32
}

func newState(path string, maxSize uint32) (*state, int64) {
	err := os.MkdirAll(path, os.ModePerm)
	if err != nil {
		panic(err)
	}

	entries, err := ioutil.ReadDir(path)
	if err != nil {
		panic(err)
	}
	maxOffset := int64(0)
	maxDirNumber := -1

	for _, entry := range entries {
		log.Printf("Encountered directory %v/%v", path, entry.Name())

		if !entry.IsDir() {
			log.Printf("Found non-directory entry %v/%v, skippng", path, entry.Name())
			continue
		}

		curDirNumber, parseErr := strconv.Atoi(entry.Name())
		if parseErr != nil {
			log.Printf("Directory %v/%v has non-number name: %v, skippng", path, entry.Name(), parseErr)
			continue
		}
		if curDirNumber > maxDirNumber {
			maxDirNumber = curDirNumber
		}

		offsetPath := fmt.Sprintf("%v/%v/offset.txt", path, entry.Name())
		offsetBytes, readErr := ioutil.ReadFile(offsetPath)
		if readErr != nil && os.IsNotExist(readErr) {
			log.Printf("Directory %v/%v does not contain offset file, skippng", path, entry.Name())
			continue
		} else if readErr != nil {
			panic(readErr)
		}
		curOffset, parseErr := strconv.ParseInt(string(offsetBytes), 10, 64)
		if parseErr != nil {
			log.Printf("File %v does not contain correct offset, skippng", offsetPath)
			continue
		}
		log.Printf("Directory %v/%v contains offset %v", path, entry.Name(), curOffset)
		if curOffset > maxOffset {
			maxOffset = curOffset
		}
	}

	log.Printf("Using directory %v/%v, offset %v", path, maxDirNumber+1, maxOffset)
	return &state{
		balance:   make(map[string]int),
		path:      path,
		dirNumber: maxDirNumber + 1,
		maxSize:   maxSize,
	}, maxOffset
}

func (s *state) dumpToDisk(offset int64) {
	type entry struct {
		key     string
		balance int
	}
	var entries []entry
	for key, curBalance := range s.balance {
		entries = append(entries, entry{
			key:     key,
			balance: curBalance,
		})
	}
	sort.Slice(entries, func(i, j int) bool {
		return entries[i].key < entries[j].key
	})

	curDirPath := fmt.Sprintf("%v/%v", s.path, s.dirNumber)
	createErr := os.Mkdir(curDirPath, os.ModePerm)
	if createErr != nil {
		panic(createErr)
	}
	dataPath := fmt.Sprintf("%v/data.txt", curDirPath)
	offsetPath := fmt.Sprintf("%v/offset.txt", curDirPath)

	var builder strings.Builder
	for _, curEntry := range entries {
		builder.WriteString(fmt.Sprintf("%v %v\n", curEntry.key, curEntry.balance))
	}

	writeErr := os.WriteFile(dataPath, []byte(builder.String()), os.ModePerm)
	if writeErr != nil {
		panic(writeErr)
	}

	offsetBytes := []byte(strconv.FormatInt(offset, 10))
	writeErr = os.WriteFile(offsetPath, offsetBytes, os.ModePerm)
	if writeErr != nil {
		panic(writeErr)
	}
}

func (s *state) processMessage(message kafka.Message) {
	key := string(message.Key)
	value, err := strconv.Atoi(string(message.Value))
	if err != nil {
		log.Printf("Cannot convert %v to int: %v", message.Value, err)
	}
	oldValue, exists := s.balance[key]
	if exists {
		s.balance[key] = oldValue + value
	} else if uint32(len(s.balance)) < s.maxSize {
		s.balance[key] = value
	} else {
		log.Printf("Dumping %v to disk...", s.balance)
		s.dumpToDisk(message.Offset)
		log.Printf("Dumped to disk...")
		s.dirNumber++
		s.balance = map[string]int{key: value}
	}
}

func main() {
	topic := "events"
	partition, err := strconv.Atoi(os.Args[1])
	if err != nil {
		panic(err)
	}

	conn, err := kafka.DialLeader(context.Background(), "tcp", "localhost:9092", topic, partition)
	if err != nil {
		panic(err)
	}
	//goland:noinspection GoUnhandledErrorResult
	defer conn.Close()

	consumerState, nextOffset := newState(fmt.Sprintf("kafka/data/partition-%v", os.Args[1]), 5)
	offset, err := conn.Seek(nextOffset, kafka.SeekAbsolute)
	if err != nil {
		panic(err)
	}
	log.Printf("Reading from %v", offset)

	for {
		batch := conn.ReadBatch(1, 1_000_000)
		for {
			msg, readErr := batch.ReadMessage()
			if readErr == io.EOF {
				break
			} else if readErr != nil {
				panic(readErr)
			}
			log.Printf(
				"Consuming { key = %v, value = %v, offset = %v }",
				string(msg.Key), string(msg.Value), msg.Offset,
			)
			consumerState.processMessage(msg)
			log.Println("Message processed")
		}

		closeErr := batch.Close()
		if closeErr != nil {
			panic(closeErr)
		}
	}
}
