// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

import java.io.PrintWriter;

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
    private final String m_where;

    public BriefPyException(Throwable wrapped, PyTraceback traceback) {
      super("", wrapped);

      m_where = "(Passed through Jython script \"" +
        traceback.tb_frame.f_code.co_filename +
        "\" at line " + traceback.tb_lineno + ")";
    }

    public void printStackTrace(PrintWriter s) {
      s.println(m_where);
      getCause().printStackTrace(s);
    }

    public String getMessage() {
      return getCause().getMessage();
    }

    public String toString() {
      return getCause().toString();
    }
  }
}
