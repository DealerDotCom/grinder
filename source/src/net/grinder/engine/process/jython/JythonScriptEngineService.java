package net.grinder.engine.process.jython;

import java.io.File;

import org.python.core.PySystemState;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.process.ScriptEngineService;
import net.grinder.util.FileExtensionMatcher;


/**
 * Jython {@link ScriptEngineService} implementation.
 *
 * @author Philip Aston
 */
public final class JythonScriptEngineService implements ScriptEngineService {

  private static final String PYTHON_HOME = "python.home";
  private static final String PYTHON_CACHEDIR = "python.cachedir";
  private static final String CACHEDIR_DEFAULT_NAME = "cachedir";

  private final FileExtensionMatcher m_pyFileMatcher =
    new FileExtensionMatcher(".py");

  // Guarded by this.
  private PySystemState m_pySystemState;

  /**
   * {@inheritDoc}
   */
  public ScriptEngine getScriptEngine(ScriptLocation script)
    throws EngineException {

    if (!m_pyFileMatcher.accept(script.getFile())) {
      return null;
    }

    synchronized (this) {
      if (m_pySystemState == null) {
        m_pySystemState = initialiseJython();
      }
    }

    return new JythonScriptEngine(m_pySystemState);
  }

  private PySystemState initialiseJython() {

    // Work around Jython issue 1894900.
    // If the python.cachedir has not been specified, and Jython is loaded
    // via the manifest classpath or the jar in the lib directory is
    // explicitly mentioned in the CLASSPATH, then set the cache directory to
    // be alongside jython.jar.
    if (System.getProperty(PYTHON_HOME) == null &&
        System.getProperty(PYTHON_CACHEDIR) == null) {
      final String classpath = System.getProperty("java.class.path");

      final File grinderJar = findFileInPath(classpath, "grinder.jar");
      final File grinderJarDirectory =
        grinderJar != null ? grinderJar.getParentFile() : new File(".");

      final File jythonJar = findFileInPath(classpath, "jython.jar");
      final File jythonHome =
        jythonJar != null ? jythonJar.getParentFile() : grinderJarDirectory;

      if (grinderJarDirectory.equals(jythonHome)) {
        final File cacheDir = new File(jythonHome, CACHEDIR_DEFAULT_NAME);
        System.setProperty("python.cachedir", cacheDir.getAbsolutePath());
      }
    }

    return new PySystemState();
  }

  /**
   * Find a file, given a search path.
   *
   * @param path The path to search.
   * @param fileName Name of the jar file to find.
   */
  private static File findFileInPath(String path, String fileName) {

    for (String pathEntry : path.split(File.pathSeparator)) {
      final File file = new File(pathEntry);

     if (file.exists() && file.getName().equals(fileName)) {
        return file;
      }
    }

    return null;
  }
}
