from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest
from HTTPClient import Codecs, NVPair
from jarray import zeros

log = grinder.logger.output

test1 = Test(1, "Upload Image")
request1 = test1.wrap(HTTPRequest(url="http://localhost:7001/"))

class TestRunner:
    def __call__(self):

        files = ( NVPair("self", "form.py"), )
        parameters = ( NVPair("run number", str(grinder.runNumber)), )

        headers = zeros(1, NVPair)
        data = Codecs.mpFormDataEncode(parameters, files, headers)
        log("Content type set to %s" % headers[0].value)

        result = request1.POST("/upload", data, headers);
