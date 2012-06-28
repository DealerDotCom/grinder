import net.grinder.script.Grinder;
import net.grinder.script.Test;

class hiWorld {
    Test test1 = new Test(1, "hello world");

    def testRunner = {
        test1.record(Grinder.grinder.logger)
        Grinder.grinder.logger.info("hello world")
    }
}