#!/usr/bin/python

import re, os

log = os.popen('git log --name-status --date=short --pretty="format:%ad  %aN %d%n%n      %s%n"  master', 'r')

previous=""
skipNext = 0

for line in log:
    if skipNext:
        skipNext = 0
        continue
    
    if re.match("^[\d]", line):
        if previous.startswith(line):
            skipNext = 1
            continue
        previous = line
        print
        
    print line,

log.close()


