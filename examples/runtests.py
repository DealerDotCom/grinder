from net.grinder.plugin.http import HTTPRequest, HTTPPluginControl as Control
from HTTPClient import NVPair
from net.grinder.script import Test


logger = grinder.getLogger()

logger.output("Hello from some script initialisation")

rawRequest1 = HTTPRequest(url="http://localhost:9001/")


test1 = Test(1, "TEST")



request1 = test1.wrap(rawRequest1)




request2 = Test(2, "Test").wrap(HTTPRequest(url="http://localhost:9001/jython/"))

defaults = Control.getConnectionDefaults()
x=Control.getConnectionDefaults("http://localhost:7001")
x.useCookies=0
defaults.useCookies=1

def scenario():
    for x in range(0, 10):
        rawRequest1.GET()

test3 = Test(3, "Scenario test").wrap(scenario)

# TestRunner() is called for every thread.
class TestRunner:
    # This is called for each run.
    def __call__(self):
        log = grinder.getLogger().output
        log("Starting test run")

        #result = test1.GET()
        #log(result.text)

        #        result = test1.GET("/examplesWebApp/SessionServlet",
        #                          [NVPair("User-Agent", "Bah")])
        
        connection = Control.getThreadConnection("http://localhost:9001")
#        connection.setProxyServer("localhost", 8001)
#        connection.timeout = 5000
#        connection.useCookies = 0
        connection.defaultHeaders = NVPair("Foo", "Bar"), NVPair("Connection", "close")
        
#        connection.followRedirects = 0

#          result = request1.GET("console")
        
#          request1.POST("console/j_security_check", (
#              NVPair("j_username", "weblogic"),
#              NVPair("j_password", "weblogic"),
#              ))

#          grinder.sleep(200)

#          result1 = request2.GET("console")

#          if (result1.statusCode == 401):
#              log("Sigh, try again")
#              connection.addBasicAuthorization("weblogic", "system", "password")
#              request2.GET("console")

        grinder.sleep(200)


        request1.GET()
        request2.GET("login.jsp")

        test3()

