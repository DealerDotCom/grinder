// Copyright (C) 2004 Philip Aston
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

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

import net.grinder.common.Logger;


/**
 * Simple logger implementation for agent processes.
 *
 * <p>Only supports the {@link Logger.TERMINAL} destination, ignores
 * instructions to write to {@link Logger.LOG}.</p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class AgentLogger implements Logger {

  private static final DateFormat s_dateFormat =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

  private final PrintWriter m_outputWriter;
  private final PrintWriter m_errorWriter;

  public AgentLogger(PrintWriter outputWriter, PrintWriter errorWriter) {
    m_outputWriter = outputWriter;
    m_errorWriter = errorWriter;
  }

  public void output(String message, int where) {
    writeMessage(m_outputWriter, message, where);
  }

  public void output(String message) {
    output(message, Logger.TERMINAL);
  }

  public void error(String message, int where) {
    writeMessage(m_errorWriter, message, where);
  }

  public void error(String message) {
    error(message, Logger.TERMINAL);
  }

  public PrintWriter getOutputLogWriter() {
    return m_outputWriter;
  }

  public PrintWriter getErrorLogWriter() {
    return m_errorWriter;
  }

  private void writeMessage(PrintWriter writer, String message, int where) {
    if (where != 0) {
      final StringBuffer formattedMessage = new StringBuffer();

      formattedMessage.append(s_dateFormat.format(new Date()));
      formattedMessage.append(" (agent): ");
      formattedMessage.append(message);

      if ((where & Logger.TERMINAL) != 0) {
        writer.println(formattedMessage);
        writer.flush();
      }
    }
  }
}
