from net.grinder.script import Test

log = grinder.logger.output

test1 = Test(1, "Log method")

class TestRunner:
    def __call__(self):
        logTest = test1.wrap(log)
        logTest("Hello World")
