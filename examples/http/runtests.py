from random import random

logger = context.getLogger()

logger.logMessage("Hello from Grinder Thread " +
                  context.getGrinderID() + "-" +
                  `context.getThreadID()`)

tests = context.getTests()

for test in tests:
    logger.logMessage(`test`)

tests[0].invoke();

for test in tests:
    test.invoke()

for x in range(10):
    if random() > 0.8:
        tests[0].invoke()
    else:
        tests[1].invoke()
