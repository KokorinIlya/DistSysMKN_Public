#!/usr/bin/python3
import sys


def main():
    stop = set()
    if len(sys.argv) > 1:
        with open(sys.argv[1], 'r') as file:
            for cur_line in file.readlines():
                cur_line = cur_line.strip()
                if cur_line != '':
                    stop.add(cur_line)
    for line in sys.stdin:
        for word in line.split():
            if word not in stop:
                print(f'{word}\t1')


if __name__ == '__main__':
    main()
