# Test script which generates some random data for testing the
# console.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from java.util import Random

r = Random()

def doIt():
    grinder.sleep(r.nextInt(100));
    pass

tests = [ Test(i, "Test %s" % i).wrap(doIt) for i in range(0, 100) ]

class TestRunner:
    def __call__(self):
        statistics = grinder.statistics
        statistics.delayReports = 1
        
        for test in tests:
            test()
            if r.nextInt() % 500 == 0:
                statistics.success = 0

        grinder.sleep(100)
