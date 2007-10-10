# HTTP cookies
#
# HTTP example which shows how to access HTTP cookies.
#
# By default, the HTTPClient library handles Cookie interaction and
# removes the cookie headers from responses. If you want to access
# them, one way is to define your own CookiePolicyHandler. This script
# defines a CookiePolicyHandler that simply logs all cookies that are
# sent or received.

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest, HTTPPluginControl
from HTTPClient import Cookie, CookieModule, CookiePolicyHandler
from java.util import Date

log = grinder.logger.output

class MyCookiePolicyHandler(CookiePolicyHandler):
    def acceptCookie(self, cookie, request, response):
        log("accept cookie: %s" % cookie)
        return 1

    def sendCookie(self, cookie, request):
        log("send cookie: %s" % cookie)
        return 1

CookieModule.setCookiePolicyHandler(MyCookiePolicyHandler())

test1 = Test(1, "Request resource")
request1 = test1.wrap(HTTPRequest())


class TestRunner:
    def __call__(self):
        result = request1.GET("http://localhost:7001/console")

        # Now let's send a cookie.
        expiryDate = Date()
        expiryDate.year = 2008
        expiryDate.month = 12
        expiryDate.date = 31

        cookie = Cookie("key", "value","localhost", "/", expiryDate, 0)

        CookieModule.addCookie(cookie,
                               HTTPPluginControl.getThreadHTTPClientContext())

        result = request1.GET("http://localhost:7001/console")
