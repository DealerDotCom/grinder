from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest
from HTTPClient import NVPair
from xml.dom import javadom
from org.xml.sax import InputSource

bookDetailsTest = Test(1, "Get book details from Amazon")
parser = javadom.XercesDomImplementation()
        
class TestRunner:
    def __call__(self):
        if grinder.runNumber > 0 or grinder.threadID > 0:
            raise RuntimeError("Use limited to one thread, one run; "
                               "see Amazon Web Services terms and conditions")
        
        request = bookDetailsTest.wrap(
            HTTPRequest(url="http://xml.amazon.com/onca/xml"))

        parameters = (
            NVPair("v", "1.0"),
            NVPair("f", "xml"),
            NVPair("t", "webservices-20"),
            NVPair("dev-t", "<insert license key here>"),
            NVPair("type", "heavy"),
            NVPair("AsinSearch", "1904284000"),
            )
        
        bytes = request.POST(parameters).inputStream

        # Parse results
        document = parser.buildDocumentUrl(InputSource(bytes))

        result = {}
        
        for details in document.getElementsByTagName("Details"):
            for detailName in ("ProductName", "SalesRank", "ListPrice"):
                result[detailName] = details.getElementsByTagName(
                    detailName)[0].firstChild.nodeValue

        grinder.logger.output(str(result))

