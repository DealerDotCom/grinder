from net.grinder.common import Logger
from net.grinder.plugin.http import HTTPTest


logger = grinder.getLogger()

logger.output("Hello from some script initialisation ")


httpTest = HTTPTest(999, "My test", url="http://localhost:9001")

notThere = HTTPTest(999, "My test2", url="http://localhost:9001/foo")

class DerivedTest(HTTPTest):
    def __init__(self):
        HTTPTest.__init__(self, 13, "Lucky 13")
        self.x = 0

    # ARG can't override getURL to hide parent version; can only
    # override non-property methods :-(
    
    def invoke(self):
        self.x += 1
        self.url="http://localhost:9001/jython/mhs.py?n=%d&thread=%d" % \
                  (self.x,grinder.getThreadID())
        return HTTPTest.invoke(self)

moreTests = [
    HTTPTest(1, "", url="http://localhost:9001/security"),
    HTTPTest(2, "", url="http://localhost:9001/security/welcome.jsp"),
    ]


# TestCase() is called for every thread.
class TestCase:
    def __init__(self):
        self.derivedTest = DerivedTest()
        pass

    # This is called for each run.
    def __call__(self):
        logger = grinder.getLogger()
        logger.output("Starting test run")

        for x in range(0,2):
            result=httpTest.invoke()

        result=self.derivedTest.invoke();
        logger.output(result.httpResponse.text)

        notThere.invoke()


