// Copyright (C) 2000 Paco Gomez
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

package net.grinder.engine.agent;

import java.io.InputStream;
import java.io.OutputStream;

import net.grinder.common.GrinderException;
import net.grinder.util.CopyStreamRunnable;


/**
 * This class knows how to start a child process. It redirects the
 * child process standard ouput and error streams to our streams.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 * @see net.grinder.engine.process.GrinderProcess
 *
 */
final class ChildProcess {

  private final Process m_process;

  /**
   * Constructor.
   *
   * @param grinderID Process identity.
   * @param commandArray Command line arguments.
   * @throws GrinderException If an error occurs.
   */
  public ChildProcess(String grinderID, String[] commandArray)
    throws GrinderException {

    try {
      m_process = Runtime.getRuntime().exec(commandArray);
    }
    catch (Exception e) {
      throw new GrinderException("Could not start process", e);
    }

    ProcessReaper.getInstance().add(m_process);

    createRedirectorThread(m_process.getInputStream(), System.out);
    createRedirectorThread(m_process.getErrorStream(), System.err);
  }

  /**
   * Return the output stream for the launched process. Blocks until
   * process is started.
   *
   * @return The stream.
   */
  public OutputStream getOutputStream() {
    return m_process.getOutputStream();
  }

  /**
   * Wait until the process has completed. Return the exit status.
   *
   * @return See {@link net.grinder.engine.process.GrinderProcess} for
   * valid values.
   * @throws GrinderException If an error occurs.
   * @throws InterruptedException If this thread is interrupted whilst
   * waiting.
   */
  public int waitFor() throws InterruptedException, GrinderException {

    m_process.waitFor();

    ProcessReaper.getInstance().remove(m_process);

    try {
      return m_process.exitValue();
    }
    catch (IllegalThreadStateException e) {
      // Can't happen.
      throw new GrinderException("Unexpected exception", e);
    }
  }

  private void createRedirectorThread(InputStream inputStream,
                                      OutputStream outputStream) {

    final Thread thread =
      new Thread(new CopyStreamRunnable(inputStream, outputStream),
                 "Stream redirector");

    thread.setDaemon(true);
    thread.start();
  }
}
