from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest

test1 = Test(1, "Request resource")
request1 = test1.wrap(HTTPRequest())

class TestRunner:
    def __call__(self):
        result = request1.GET("http://localhost:7001/")
        writeToFile(result.text)

def writeToFile(text):
    filename = grinder.getFilenameFactory().createFilename(
        "page", "-%d.html" % grinder.runNumber)

    file = open(filename, "w")
    print >> file, text
    file.close()
