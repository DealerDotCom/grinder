#!/bin/python
# -*- coding: iso8859-1 -*-

"""
Prototype of the slow buffer algorithm.
"""

import random

class SlowClientBufferGrowthStrategy:
    
    def __init__(self, targetBaud, dampingFactor):
        self.targetBaud = targetBaud
        self.dampingFactor = dampingFactor
        self.sleepTime = 0
        self.lastBufferResizeTime = 0
        self.lastPosition = 0
    
    def calculateSleepTime(self, now, position):
        if position != 0:
            timeSinceLastResize = now - self.lastBufferResizeTime
            
            self.sleepTime += \
                ((position - self.lastPosition) * 8 * 1000 / self.targetBaud \
                - timeSinceLastResize) * self.dampingFactor;
            
            if self.sleepTime < 0: self.sleepTime = 0
            
        self.lastPosition = position          
        self.lastBufferResizeTime = now
        
        return self.sleepTime


def run():
    s = SlowClientBufferGrowthStrategy(56000, 0.67)
    
    now = 100
    increment = 100
    workTime = 10
    
    def baud(bytes, milliseconds):
        if milliseconds: return bytes * 8 * 1000 / milliseconds
        else: return 0
    
    for run in range(1, 4):
        print "***** RUN %d *****" % run
        position = 0
        start = now
        
        for x in range(0, 20):
            sleepTime = s.calculateSleepTime(now, position)
            
            cumulativeBaud = baud(position, (now - start))
            
            instantBaud = baud(increment, sleepTime + workTime)
            print "%6d %6d: %5d -> %d %d" % (now, position, sleepTime, cumulativeBaud, instantBaud)
            position += increment
            now += sleepTime + workTime
            
            workTime = random.normalvariate(10, 5)
            increment = random.randint(90, 100)
        

if __name__ == "__main__":
    run()
