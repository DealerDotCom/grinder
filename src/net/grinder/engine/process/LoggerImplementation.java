// Copyright (C) 2001, 2002, 2003 Philip Aston
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Date;

import net.grinder.common.Logger;
import net.grinder.engine.EngineException;
import net.grinder.util.DelayedCreationFileWriter;


/**
 * Logger implementation.
 *
 * <p>Each thread should call {@link #getProcessLogger} or {@link
 * #createThreadLogger} to get a {@link net.grinder.common.Logger}.
 * </p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class LoggerImplementation {
  private static final PrintWriter s_stdoutWriter;
  private static final PrintWriter s_stderrWriter;
  private static final char[] s_lineSeparator =
    System.getProperty("line.separator").toCharArray();
  private static final DateFormat s_dateFormat =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

  private static String s_dateString;

  static {
    s_stdoutWriter = new PrintWriter(System.out);
    s_stderrWriter = new PrintWriter(System.err);
  }

  private static int s_currentTick = 0;
  private static int s_lastTick = -1;

  /** Regularly incremented by GrinderProcess. */
  static void tick() {
    ++s_currentTick;
  }

  /** Use our DateFormat at most once a tick. Don't synchronise, who
   * cares if its wrong?
   */
  private static /* synchronized */ String getDateString() {

    if (s_lastTick != s_currentTick) {
      s_dateString = s_dateFormat.format(new Date());
      s_lastTick = s_currentTick;
    }

    return s_dateString;
  }

  private final String m_grinderID;
  private final boolean m_logProcessStreams;
  private final FilenameFactoryImplementation m_filenameFactory;
  private final PrintWriter m_outputWriter;
  private final PrintWriter m_errorWriter;
  private final PrintWriter m_dataWriter;
  private final Logger m_processLogger;
  private boolean m_errorOccurred = false;

  LoggerImplementation(String grinderID, String logDirectoryString,
                       boolean logProcessStreams, boolean appendLog)
    throws EngineException {
    m_grinderID = grinderID;
    m_logProcessStreams = logProcessStreams;

    final File logDirectory = new File(logDirectoryString, "");

    try {
      logDirectory.mkdirs();
    }
    catch (Exception e) {
      throw new EngineException(e.getMessage(), e);
    }

    if (!logDirectory.canWrite()) {
      throw new EngineException("Cannot write to log directory '" +
                                logDirectory.getPath() + "'");
    }

    m_filenameFactory =
      new FilenameFactoryImplementation(logDirectory, grinderID);

    // Although we manage the flushing ourselves and don't call
    // println, we set auto flush on these PrintWriters because
    // clients can get direct access to them.
    m_outputWriter = new PrintWriter(createWriter("out", appendLog), true);
    m_errorWriter =
      new PrintWriter(createWriter("error", appendLog), true);

    // Don't autoflush, we explictly control flushing of this writer.
    m_dataWriter = new PrintWriter(createWriter("data", appendLog), false);

    m_processLogger = createThreadLogger(-1);
  }

  private Writer createWriter(String prefix, boolean appendLog)
    throws EngineException {
    final File file = new File(m_filenameFactory.createFilename(prefix));

    // Check we can write to the file and moan now. We won't see
    // the problem later because PrintWriters eat exceptions. If
    // the file doesn't exist, we're pretty sure we can create it
    // because we checked we can write to the log directory.
    if (file.exists() && !file.canWrite()) {
      throw new EngineException("Cannot write to '" + file.getPath() +
                                "'");
    }

    return new BufferedWriter(
      new DelayedCreationFileWriter(file, appendLog));
  }

  Logger getProcessLogger() throws EngineException {
    return m_processLogger;
  }

  ThreadLogger createThreadLogger(int threadID) {
    return new ThreadState(threadID);
  }

  FilenameFactoryImplementation getFilenameFactory()  {
    return m_filenameFactory;
  }

  PrintWriter getDataWriter() {
    return m_dataWriter;
  }

  private void outputInternal(ThreadState state, String message,
                                    int where) {
    int w = where;

    if (!m_logProcessStreams) {
      w &= ~Logger.LOG;
    }

    if (w != 0) {
      final int lineLength = state.formatMessage(message);

      if ((w & Logger.LOG) != 0) {
        m_outputWriter.write(state.m_outputLine, 0, lineLength);
        m_outputWriter.flush();
      }

      if ((w & Logger.TERMINAL) != 0) {
        s_stdoutWriter.write(state.m_outputLine, 0, lineLength);
        s_stdoutWriter.flush();
      }
    }
  }

  private void errorInternal(ThreadState state, String message, int where) {

    int w = where;

    if (!m_logProcessStreams) {
      w &= ~Logger.LOG;
    }

    if (w != 0) {
      final int lineLength = state.formatMessage(message);

      if ((w & Logger.LOG) != 0) {
        m_errorWriter.write(state.m_outputLine, 0, lineLength);
        m_errorWriter.flush();
      }

      if ((w & Logger.TERMINAL) != 0) {
        s_stderrWriter.write(state.m_outputLine, 0, lineLength);
        s_stderrWriter.flush();
      }

      final int summaryLength = 20;

      final String summary =
        message.length() > summaryLength ?
        message.substring(0, summaryLength) + "..." : message;

      outputInternal(state,
                     "ERROR (\"" + summary +
                     "\"), see error log for details",
                     Logger.LOG);

      if (!m_errorOccurred && (w | Logger.TERMINAL) != 0) {
        m_processLogger.output(
          "There were errors, see error log for details",
          Logger.TERMINAL);

        m_errorOccurred = true;
      }
    }
  }

  /**
   * Thread specific state.
   *
   * <p>We declare that we implement {@link net.grinder.common.Logger}
   * as well as {@link ThreadLogger} because <code>ThreadLogger</code>
   * is package scope and this prevents Jython from seeing
   * <code>Logger</code> (<code>IllegalAccessException</code>s
   * abound.</p>
   **/
  private final class ThreadState implements Logger, ThreadLogger {
    private final int m_threadID;
    private int m_currentRunNumber = -1;
    private int m_currentTestNumber = -1;

    // Scratch space.
    private final StringBuffer m_buffer = new StringBuffer();
    private final char[] m_outputLine = new char[512];

    // Reused for optimisation.
    private final char[] m_processOrThreadIDCharacters;
    private char[] m_currentRunNumberCharacters = null;

    public ThreadState(int threadID) {
      m_threadID = threadID;

      m_buffer.setLength(0);

      if (m_threadID == -1) {
        m_buffer.append(" (process ");
        m_buffer.append(m_grinderID);
        m_buffer.append("): ");
      }
      else {
        m_buffer.append(" (thread ");
        m_buffer.append(m_threadID);
      }

      m_processOrThreadIDCharacters = getBufferCharacters(0);
    }

    private char[] getBufferCharacters(int start) {

      final int length = m_buffer.length();
      final char[] result = new char[length - start];
      m_buffer.getChars(start, length, result, 0);
      return result;
    }

    public int getThreadID() {
      return m_threadID;
    }

    public int getCurrentRunNumber() {
      return m_currentRunNumber;
    }

    public void setCurrentRunNumber(int runNumber) {
      if (runNumber != m_currentRunNumber) {
        m_currentRunNumberCharacters = null;
      }

      m_currentRunNumber = runNumber;
    }

    public int getCurrentTestNumber() {
      return m_currentTestNumber;
    }

    public void setCurrentTestNumber(int testNumber) {
      m_currentTestNumber = testNumber;
    }

    public void output(String message) {
      outputInternal(this, message, Logger.LOG);
    }

    public void output(String message, int where) {
      outputInternal(this, message, where);
    }

    public void error(String message) {
      errorInternal(this, message, Logger.LOG);
    }

    public void error(String message, int where) {
      errorInternal(this, message, where);
    }

    public PrintWriter getOutputLogWriter() {
      return m_outputWriter;
    }

    public PrintWriter getErrorLogWriter() {
      return m_errorWriter;
    }

    int formatMessage(String message) {
      m_buffer.setLength(0);

      m_buffer.append(getDateString());

      m_buffer.append(m_processOrThreadIDCharacters);

      if (m_threadID != -1) {
        // We're a real thread, bolt on the rest of the context.

        if (m_currentRunNumber >= 0) {
          if (m_currentRunNumberCharacters == null) {
            final int start = m_buffer.length();
            m_buffer.append(" run ");
            m_buffer.append(Integer.toString(m_currentRunNumber));
            m_currentRunNumberCharacters = getBufferCharacters(start);
          }
          else {
            m_buffer.append(m_currentRunNumberCharacters);
          }
        }

        if (m_currentTestNumber >= 0) {
          // We don't cache the test number part of the string because
          // it is rarely used twice.
          m_buffer.append(" test ");
          m_buffer.append(Integer.toString(m_currentTestNumber));
        }

        m_buffer.append("): ");
      }

      m_buffer.append(message);

      // Sadly this is the most efficient way to get something we can
      // println from the StringBuffer. getString() creates an extra
      // string, getValue() is package scope.
      final int bufferLength = m_buffer.length();
      final int outputLineSpace = m_outputLine.length - s_lineSeparator.length;

      final int lineLength =
        bufferLength > outputLineSpace ? outputLineSpace : bufferLength;

      m_buffer.getChars(0, lineLength, m_outputLine, 0);

      System.arraycopy(s_lineSeparator, 0, m_outputLine, lineLength,
                       s_lineSeparator.length);

      return lineLength + s_lineSeparator.length;
    }
  }
}
