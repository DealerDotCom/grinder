# An example JMS receiver.
#
# JMS objects are looked up and messages are created once during
# initialisation. This default JNDI names are for the WebLogoic Server
# 7.0 examples domain - change accordingly.
#
# Each worker thread:
#  - Creates a queue session
#  - Recieves ten messages
#  - Closes the queue session
#
# This script demonstrates the use of The Grinder statistics API to
# record a "delivery time" statistic.

from java.lang import System
from java.util import Properties
from javax.jms import MessageListener, Session
from javax.naming import Context, InitialContext
from net.grinder.script import Test
from net.grinder.statistics import ExpressionView, StatisticsIndexMap, StatisticsView
from threading import Condition
from weblogic.jndi import WLInitialContextFactory

log = grinder.logger.output

# Look up connection factory and queue in JNDI. 
properties = Properties()
properties[Context.PROVIDER_URL] = "t3://localhost:7001"
properties[Context.INITIAL_CONTEXT_FACTORY] = WLInitialContextFactory.name

initialContext = InitialContext(properties)

connectionFactory = initialContext.lookup("weblogic.examples.jms.QueueConnectionFactory")
queue = initialContext.lookup("weblogic.examples.jms.exampleQueue")
initialContext.close()

# Create a connection.
connection = connectionFactory.createQueueConnection()
connection.start()

# Use userLong0 statistic to represent the "delivery time".
deliveryTimeIndex = StatisticsIndexMap.getInstance().getIndexForLong("userLong0")

# Add two statistics views:
# 1. Delivery time:- the mean time taken between the server sending
# the message and the receiver receiving the message.
# 2. Mean delivery time:- the delivery time averaged over all
# transactions.

detailView = StatisticsView()
detailView.add(ExpressionView("Delivery time", "", "userLong0"))
	    
summaryView = StatisticsView()
summaryView.add(ExpressionView(
    "Mean delivery time",
    "statistic.deliveryTime",
    "(/ userLong0(+ timedTransactions untimedTransactions))"))
	    
grinder.registerDetailStatisticsView(detailView)
grinder.registerSummaryStatisticsView(summaryView)

# We record each message receipt against a single test. The
# transaction time is meaningless.
def recordDeliveryTime(deliveryTime):
    grinder.currentTestStatistics.addValue(deliveryTimeIndex, deliveryTime)

recordTest = Test(1, "Receive messages").wrap(recordDeliveryTime)
        
class TestRunner(MessageListener):
    def __init__(self):
        self.receivedMessages = 0
        # Use a Condition to synchronise thread activity.              
        self.cv = Condition()

    def __call__(self):
        log("Creating queue session and a receiver")
        session = connection.createQueueSession(0, Session.AUTO_ACKNOWLEDGE)

        receiver = session.createReceiver(queue)
        receiver.messageListener = self

        # Read ten messages from the queue.
        for i in range(0, 10):

            # Wait until we have received a message.            
            self.cv.acquire()
            while self.receivedMessages == 0: self.cv.wait()
            self.receivedMessages -= 1
            self.cv.release()

            log("Received message")            

            # We record the test a here rather than in onMessage
            # because we must do so from a worker thread.
            recordTest(self.lastDeliveryTime)            
            
        log("Closing queue session")
        session.close()        

    # Called asynchronously by JMS when a message arrives.
    def onMessage(self, message):
        self.cv.acquire()
        
        self.receivedMessages += 1

        # In WebLogic Server JMS, the JMS timestamp is set by the
        # sender session. All we need to do is ensure our clocks are
        # synchronised...
        self.lastDeliveryTime = System.currentTimeMillis() - message.getJMSTimestamp()
        self.cv.notifyAll()
        self.cv.release()

        
