from net.grinder.script import Test
from examples.webservices.basic.javaclass import HelloWorld_Impl
from java.lang import System

System.setProperty( "javax.xml.rpc.ServiceFactory",
 "weblogic.webservice.core.rpc.ServiceFactoryImpl");

webService = HelloWorld_Impl("http://localhost:7001/basic_javaclass/HelloWorld?WSDL");

port  = webService.getHelloWorldPort();
portTest = Test(1, "JAXP Port test").wrap(port)


class BadMojo(Exception): pass

class TestRunner:
    def __call__(self):
        result = portTest.sayHello(grinder.threadID, grinder.grinderID);
        grinder.logger.output("Got '%s'" % result);
        raise BadMojo()
