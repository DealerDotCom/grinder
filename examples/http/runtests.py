from net.grinder.common import Logger
from net.grinder.plugin.http import HTTPPlugin,HTTPTest
from HTTPClient import NVPair


logger = grinder.getLogger()

logger.output("Hello from some script initialisation")

test1 = HTTPTest(1, "Test",url="http://localhost:7001/")
test2 = HTTPTest(2, "Test",url="http://localhost:9001/")

plugin = grinder.getPlugin(HTTPPlugin)

defaults = plugin.getDefaultConnectionDefaults()
x=plugin.getConnectionDefaults("http://localhost:7001")
x.useCookies=0
defaults.useCookies=1

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
        
        connection = plugin.getConnection("http://localhost:9001")
        connection.setProxyServer("localhost", 8001)
#        connection.timeout = 100
#        connection.useCookies = 0
        connection.defaultHeaders = NVPair("Foo", "Bar"), NVPair("Connection", "close")
        
#        connection.followRedirects = 0
 
        test1.GET("console")
        test1.POST("console/j_security_check", (
            NVPair("j_username", "weblogic"),
            NVPair("j_password", "weblogic"),
            ))

        result1 = test2.GET("console")

        if (result1.statusCode == 401):
            log("Sigh, try again")
            connection.addBasicAuthorization("weblogic", "system", "password")
            test2.GET("console")

#        test1.GET("/examplesWebApp/SessionServlet")
#        test1.GET("/examplesWebApp","Yep= 21 12")
        #for x in range(0,2):
        #test2.GET("%s?x=%d" % (url, x))







