from random import random

logger = grinder.getLogger()

logger.logMessage("Hello from Grinder Thread " +
                  grinder.getGrinderID() + "-" +
                  `grinder.getThreadID()`)

tests = grinder.getTests()

for test in tests:
    logger.logMessage(`test`)

result = tests[0].invoke();

if result.isSuccessful():
    print("The test worked")
else:
    print("The test failed")

for test in tests:
    test.invoke()

for x in range(10):
    if random() > 0.8:
        tests[0].invoke()
    else:
        tests[1].invoke()
