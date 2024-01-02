#!/usr/bin/python3
import sys


def main():
    cur_word = None
    cur_sum = 0
    for line in sys.stdin:
        new_word, count = line.split('\t')
        if cur_word is not None and new_word != cur_word:
            print(f'{cur_word}\t{cur_sum}')
            cur_sum = 0
        cur_word = new_word
        cur_sum += int(count)
    if cur_word is not None:
        print(f'{cur_word}\t{cur_sum}')


if __name__ == '__main__':
    main()
