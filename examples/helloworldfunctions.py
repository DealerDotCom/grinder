from net.grinder.script import Test

log = grinder.logger.output

test1 = Test(1, "Log method")

def doRun():
    logTest = test1.wrap(log)
    logTest("Hello World")

def TestRunner():
    return doRun
