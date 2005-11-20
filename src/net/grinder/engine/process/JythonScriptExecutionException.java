// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyTraceback;

import net.grinder.engine.common.EngineException;


/**
 * Exception that wraps errors encountered when invoking Jython
 * scripts.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class JythonScriptExecutionException extends EngineException {
  /**
   * Creates a new <code>JythonScriptExecutionException</code> instance.
   *
   * @param doingWhat What we were doing.
   * @param e <code>PyException</code> that we caught.
   */
  public JythonScriptExecutionException(String doingWhat, PyException e) {
    super("Jython error encountered " + doingWhat, stripPyException(e));
  }

  private static Throwable stripPyException(PyException e) {
    final Object javaError = e.value.__tojava__(Throwable.class);

    if (javaError == null || javaError == Py.NoConversion) {
      return e;
    }

    return new BriefPyException((Throwable)javaError, e.traceback);
  }

  /**
   * Remove any JythonScriptExecutionException wrapping and return
   * the underlying exception thrown from the script.
   */
  final Throwable unwrap() {
    Throwable result = this;

    do {
      result = ((EngineException)result).getCause();
    }
    while (result instanceof JythonScriptExecutionException ||
           result instanceof BriefPyException);

    return result;
  }

  /**
   * Used to replace PyExceptions that encapsulate Java exceptions.
   * (The PyException stack trace is much too verbose my tastes and
   * repeats a lot of information found in the Java exception).
   */
  private static final class BriefPyException extends EngineException {
    public BriefPyException(Throwable wrapped, PyTraceback traceback) {
      super(tracebackToMessage(traceback), wrapped);
      setStackTrace(new StackTraceElement[0]);
    }

    /**
     * Remove the class name from stack traces.
     */
    public String toString() {
      return getLocalizedMessage();
    }
  }

  /**
   * We fix various following problems with PyTraceback.dumpStack() to make it
   * more suitable for incorporation with a Java stack trace.
   * <ul>
   * <li>PyTraceback doesn't use platform specific line separators.</li>
   * <li>Stacks are printed with the innermost frame last.</li>
   * <li>The indentation style is different.</li>
   * </ul>
   */
  private static String tracebackToMessage(PyTraceback traceback) {
    final StringBuffer result = new StringBuffer("Jython traceback");

    final String[] frames = traceback.dumpStack().split("\n");

    for (int i = frames.length - 1; i >= 1; --i) {
      result.append(System.getProperty("line.separator"));
      result.append("\t");
      result.append(frames[i].trim());
    }

    return result.toString();
  }
}
