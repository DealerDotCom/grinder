from net.grinder.plugin.http import HTTPTest


logger = grinder.getLogger()

logger.logMessage("Hello from some script initialisation ")


httpTest = HTTPTest(999, "My test", "http://localhost:9001")

class A:
    pass

# Not allowed - should be??? If not, make failure nicer.
#httpTest.invoke()

moreTests = [
    HTTPTest(1, "", "http://localhost:9001/security"),
    HTTPTest(2, "", "http://localhost:9001/security/welcome.jsp"),
    ]
                  
# Style 1
class TestCase:
    def __init__(self):
        self.x=0
        
        raise A()

     
    def __call__(self):
        global logger
        logger.logMessage("Starting test run")

        for x in range(0,10):
            httpTest.invoke()

        self.x += 1


# Style 2
#  def run():
#      global g
#      print "Hello", g
#      g+=1
        
#  def TestCase():
#      return run

