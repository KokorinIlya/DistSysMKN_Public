package main

import "fmt"

type Month uint8

const (
	January   Month = 1
	February  Month = 2
	March     Month = 3
	April     Month = 4
	May       Month = 5
	June      Month = 6
	July      Month = 7
	August    Month = 8
	September Month = 9
	October   Month = 10
	November  Month = 11
	December  Month = 12
)

type Date struct {
	Year  int16
	Month Month
	Day   uint8
}

type Author struct {
	Name      string
	BirthDate Date
}

type Book struct {
	Title    string
	Author   Author
	Language string
}

type Library struct {
	Address string
	Books   []Book
}

func main() {
	babylonLibrary := Library{
		Address: "Babylon",
		Books: []Book{
			{
				Title:    "In Stahlgewittern",
				Language: "German",
				Author: Author{
					Name:      "Ernst Jünger",
					BirthDate: Date{Year: 1895, Month: March, Day: 29},
				},
			},
			{
				Title:    "Опавшие листья",
				Language: "Russian",
				Author: Author{
					Name:      "Василий Розанов",
					BirthDate: Date{Year: 1856, Month: May, Day: 2},
				},
			},
			{
				Title:    "Rigodon",
				Language: "French",
				Author: Author{
					Name:      "Louis-Ferdinand Céline",
					BirthDate: Date{Year: 1894, Month: May, Day: 27},
				},
			},
		},
	}
	fmt.Println(babylonLibrary)
}
