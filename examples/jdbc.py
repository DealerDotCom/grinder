# Some simple database playing with JDBC.
#
# To run this, set the Oracle login details appropriately and add the
# Oracle thin driver classes to your CLASSPATH.

from java.sql import DriverManager
from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from oracle.jdbc import OracleDriver

test1 = Test(1, "Database insert")
test2 = Test(2, "Database query")

# Load the Oracle JDBC driver.
DriverManager.registerDriver(OracleDriver())

def getConnection():
    return DriverManager.getConnection(
        "jdbc:oracle:thin:@127.0.0.1:1521:mysid", "wls", "wls")

def ensureClosed(object):
    try: object.close()
    except: pass

# One time initialisation that cleans out old data.
connection = getConnection()
statement = connection.createStatement()

try: statement.execute("drop table grinder_fun")
except: pass

statement.execute("create table grinder_fun(thread number, run number)")

ensureClosed(statement)
ensureClosed(connection)
    
class TestRunner:
    def __call__(self):
        connection = None
        statement = None

        try:
	    connection = getConnection()
	    statement = connection.createStatement()
            
            testInsert = test1.wrap(statement)
            testInsert.execute("insert into grinder_fun values(%d, %d)" %
                               (grinder.threadID, grinder.runNumber))

            testQuery = test2.wrap(statement)
            testQuery.execute("select * from grinder_fun where thread=%d" %
                              grinder.threadID)

	finally:
            ensureClosed(statement)
	    ensureClosed(connection)
