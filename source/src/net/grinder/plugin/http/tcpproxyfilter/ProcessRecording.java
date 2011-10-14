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

package net.grinder.plugin.http.tcpproxyfilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;

import net.grinder.common.GrinderException;
import net.grinder.common.Logger;
import net.grinder.plugin.http.tcpproxyfilter.ProcessHTTPRecordingWithXSLT.StyleSheetInputStream;
import net.grinder.plugin.http.xml.HttpRecordingDocument;
import net.grinder.util.AbstractMainClass;
import net.grinder.util.FixedWidthFormatter;
import net.grinder.util.SimpleLogger;


/**
 * Command line invocation for testing purposes.
 *
 * @author Philip Aston
 */
public final class ProcessRecording extends AbstractMainClass {

  private static final String USAGE =
    "\n  java " + ProcessRecording.class + " recording [stylesheet]" +
    "\n\n";

  /**
   * Entry point.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    final Logger logger =
      new SimpleLogger("tcpproxy",
                       new PrintWriter(System.out),
                       new PrintWriter(System.err),
                       new FixedWidthFormatter(
                         FixedWidthFormatter.Align.LEFT,
                         FixedWidthFormatter.Flow.WORD_WRAP,
                         80));

    try {
      final ProcessRecording process = new ProcessRecording(args, logger);
      process.run();
    }
    catch (LoggedInitialisationException e) {
      System.exit(1);
    }
    catch (Throwable e) {
      logger.error("Could not initialise:");
      final PrintWriter errorWriter = logger.getErrorLogWriter();
      e.printStackTrace(errorWriter);
      errorWriter.flush();
      System.exit(2);
    }

    System.exit(0);
  }

  private final ProcessHTTPRecordingWithXSLT m_processor;
  private final InputStream m_recordingStream;

  private ProcessRecording(String[] arguments, Logger logger)
    throws GrinderException {

    super(logger, USAGE);

    try {
      m_recordingStream = new FileInputStream(arguments[0]);
      if (arguments.length == 1) {
        m_processor = new ProcessHTTPRecordingWithXSLT(logger);
      }
      else if (arguments.length == 2) {
        m_processor =
          new ProcessHTTPRecordingWithXSLT(
            new StyleSheetInputStream(new File(arguments[1])),
            getLogger());
      }
      else {
        throw barfUsage();
      }
    }
    catch (FileNotFoundException fnfe) {
      throw barfError(fnfe.getMessage());
    }
    catch (IndexOutOfBoundsException e) {
      throw barfUsage();
    }
  }

  private void run() throws Exception {

    final HttpRecordingDocument recording =
      HttpRecordingDocument.Factory.parse(m_recordingStream);

    m_processor.process(recording);
  }

}
