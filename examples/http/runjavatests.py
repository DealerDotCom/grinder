from java.util import Properties,Random,HashMap
from javax.naming import Context,InitialContext
from net.grinder.plugin.http import HTTPTest
from net.grinder.plugin.java import JavaPlugin,JavaTest
from weblogic.jndi import WLInitialContextFactory
from net.grinder.common import GrinderProperties


# Module level code contains one-time initialisation. The "grinder"
# object provides access to The Grinder engine.
grinder.logger.output("Hello from some script initialisation")

p = Properties()
p[Context.INITIAL_CONTEXT_FACTORY] = WLInitialContextFactory.name

plugin = grinder.getPlugin(JavaPlugin)

home = InitialContext(p).lookup("ejb20-statefulSession-TraderHome")
homeProxy = JavaTest(1, "TraderHome").createProxy(home)

random = Random()

def doSomeStuff(map):
    for x in range(0,100):
        map.get("foo")

# The engine calls TestRunner() for every thread
class TestRunner:

    # Do any per-thread initialisation in constructor
    def __init__(self):
        pass 

    # This is called for each run
    def __call__(self):

        # Alias a logger function that logs thread details
        log = grinder.logger.output

        # Invoke test 1
        trader = homeProxy.create()

        # Create a test wrapper to record all "sell" and "buy" invocations
        tradeProxy = JavaTest(2, "Trader sell/buy").createProxy(trader)

        # Create a test wrapper to record "get balance" invocations
        queryProxy = JavaTest(3, "Trader query").createProxy(trader)

        stocksToSell = { "BEAS" : 100, "MSFT" : 999 }
        for stock, amount in stocksToSell.items():
            # Test 2            
            tradeResult = tradeProxy.sell("John", stock, amount)
            log(str(tradeResult))

        stocksToBuy = { "BEAS" : abs(random.nextInt()) % 1000 }
        for stock, amount in stocksToBuy.items():
            # This is also recorded as test 2            
            tradeResult = tradeProxy.buy("Phil", stock, amount)
            log(str(tradeResult))

        # Idle a while
        grinder.sleep(100)

        map = HashMap()
        mapProxy = JavaTest(4, "Test HashMap").createProxy(map)
        for x in range(0,100):
            mapProxy.get("foo")

        mapProxy2 = JavaTest(5, "Test HashMap2").createProxy(doSomeStuff)
        mapProxy2(map)

        # Test 3            
        balance = queryProxy.getBalance();
        log("Balance is $%.2f" % balance)

        # We don't bother recording the remove as a test
        trader.remove()

        # Can obtain information about the thread context...
        if grinder.threadID == 0 and grinder.runNumber == 0:
            # ...and navigate from the proxy back to the test
            d = queryProxy.__test__
            log("Query test is test %d, (%s)" % (d.number, d.description))






            
