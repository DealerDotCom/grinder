package net.grinder.scriptengine.groovy;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.util.Directory;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static net.grinder.testutility.AssertUtilities.assertContains;
import static net.grinder.testutility.FileUtilities.createFile;
import static org.junit.Assert.fail;

/**
 * @author Ryan Gardner
 */
public class TestGroovyScriptEngine extends AbstractJUnit4FileTestCase {

    private static int s_called;

    @Test
    public void testEngineCreationWithBasicClosure() throws IOException, EngineException {
        final ScriptLocation script =
                new ScriptLocation(new Directory(getDirectory()), new File("test.groovy"));

        createFile(script.getFile(),
                "class foo {" +
                        "def testRunner = { println \"called runner\"}" +
                        "}");

        new GroovyScriptEngineService().createScriptEngine(script);
    }

    @Test
    public void testEngineCreationWithNoClassWrappingClosure() throws IOException, EngineException {
        final ScriptLocation script =
                new ScriptLocation(new Directory(getDirectory()), new File("testNoWrappingClass.groovy"));

        createFile(script.getFile(),"def testRunner = { println \"called runner\"}");
        try {
            new GroovyScriptEngineService().createScriptEngine(script);
            fail("Expected ScriptExecutionException because there is no class wrapping the closure");
        }
        catch (EngineException e) {
            assertContains(e.getMessage(),"Unable to locate the closure named");
        }
    }

    @Test
    public void testEngineCreationWithNoClosure() throws IOException, EngineException {
        final ScriptLocation script =
                new ScriptLocation(new Directory(getDirectory()), new File("testNoClosure.groovy"));

        createFile(script.getFile(),"class foo {  }");
        try {
            new GroovyScriptEngineService().createScriptEngine(script);
            fail("Expected ScriptExecutionException because there is no class wrapping the closure");
        }
        catch (EngineException e) {
            assertContains(e.getMessage(),"Unable to locate the closure named");
        }
    }

}
