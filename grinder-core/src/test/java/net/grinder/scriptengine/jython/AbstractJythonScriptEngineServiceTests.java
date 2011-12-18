// Copyright (C) 2011 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.scriptengine.jython;

import static net.grinder.testutility.FileUtilities.createFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import net.grinder.common.StubTest;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.Recorder;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.util.Directory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Common tests for Jython script engine services.
 *
 * @author Philip Aston
 */
public abstract class AbstractJythonScriptEngineServiceTests
  extends AbstractJUnit4FileTestCase {

  protected final net.grinder.common.Test m_test =
    new StubTest(1, "foo");

  protected ScriptLocation m_pyScript;

  protected JythonScriptEngineService m_jythonScriptEngineService =
    new JythonScriptEngineService();

  @Mock protected Recorder m_recorder;

  public AbstractJythonScriptEngineServiceTests() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    m_pyScript =
      new ScriptLocation(new Directory(getDirectory()), new File("some.py"));

    createFile(m_pyScript.getFile(),
               "class TestRunner: pass");
  }

  @Test
  public void testInitialisationWithEmptyClasspath() throws Exception {
    System.clearProperty("python.cachedir");
    final String originalClasspath = System.getProperty("java.class.path");

    System.setProperty("java.class.path", "");

    try {
      m_jythonScriptEngineService.createScriptEngine(m_pyScript);

      assertNotNull(System.getProperty("python.cachedir"));
      System.clearProperty("python.cachedir");
    }
    finally {
      System.setProperty("java.class.path", originalClasspath);
      System.clearProperty("python.cachedir");
    }
  }

  @Test public void testInitialisationWithNonCollocatedGrinderAndJythonJars()
    throws Exception {

    final String temporaryCacheDir = getDirectory().getPath();

    System.setProperty("python.cachedir", temporaryCacheDir);

    m_jythonScriptEngineService.createScriptEngine(m_pyScript);

    assertEquals(temporaryCacheDir, System.getProperty("python.cachedir"));
  }

  @Test public void testInitialisationWithCacheDir() throws Exception {

    System.clearProperty("python.cachedir");
    final String originalClasspath = System.getProperty("java.class.path");

    final File anotherDirectory = new File(getDirectory(), "foo");
    anotherDirectory.mkdirs();
    final File grinderJar = new File(anotherDirectory, "grinder.jar");
    grinderJar.createNewFile();
    final File jythonJar = new File(getDirectory(), "jython.jar");
    jythonJar.createNewFile();

    System.setProperty("java.class.path",
                       grinderJar.getAbsolutePath() + File.pathSeparator +
                       jythonJar.getAbsolutePath());

    try {
      m_jythonScriptEngineService.createScriptEngine(m_pyScript);
      assertNull(System.getProperty("python.cachedir"));
    }
    finally {
      System.setProperty("java.class.path", originalClasspath);
    }
  }

  @Test public void testNoMatch() throws Exception {
    final ScriptLocation notPyScript = new ScriptLocation(new File("blah.clj"));

    assertNull(m_jythonScriptEngineService.createScriptEngine(notPyScript));
  }

}
