from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest
from HTTPClient import NVPair

log = grinder.logger.output

protectedResourceTest = Test(1, "Request resource")
authenticationTest = Test(2, "POST to j_security_check")

class TestRunner:
    def __call__(self):
        request = protectedResourceTest.wrap(
            HTTPRequest(url="http://localhost:7001/console"))

        result = request.GET()
        
        result = maybeAuthenticate(result)

        result = request.GET()

def maybeAuthenticate(lastResult):
    """Function that checks the given result and performs J2EE Form
    Based authentication if necessary."""

    if lastResult.statusCode == 401 \
    or lastResult.text.find("j_security_check") != -1:
        
        log("Challenged, authenticating")

        authenticationFormData = ( NVPair("j_username", "weblogic"),
                                   NVPair("j_password", "weblogic"),)

        request = authenticationTest.wrap(
            HTTPRequest(url="%s/j_security_check" % lastResult.originalURI))
            
        return request.POST(authenticationFormData)
