import net.grinder.script.Grinder;
import net.grinder.script.Test
import groovy.transform.CompileStatic

// the class name is arbitrary
class hiWorld {
    Test test1 = new Test(1, "hello world")
    test1.record (Grinder.grinder.logger)

    // the closure named "testRunner" is what will be called
    // for each test
    def testRunner = {
        Grinder.grinder.logger.info("hello world")
    }
}