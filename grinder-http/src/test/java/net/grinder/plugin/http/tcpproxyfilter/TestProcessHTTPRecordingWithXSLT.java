// Copyright (C) 2005 - 2011 Philip Aston
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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

import net.grinder.common.Logger;
import net.grinder.plugin.http.tcpproxyfilter.ProcessHTTPRecordingWithXSLT.BuiltInStyleSheet;
import net.grinder.plugin.http.tcpproxyfilter.ProcessHTTPRecordingWithXSLT.StyleSheetFile;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HttpRecordingDocument;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.RedirectStandardStreams;
import net.grinder.util.StreamCopier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link ProcessHTTPRecordingWithXSLT}.
 *
 * @author Philip Aston
 */
public class TestProcessHTTPRecordingWithXSLT
  extends AbstractJUnit4FileTestCase {

  @Mock private Logger m_logger;
  private StringWriter m_out = new StringWriter();

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    when (m_logger.getOutputLogWriter()).thenReturn(new PrintWriter(m_out));
  }

  @Test public void testWithIdentityTransform() throws Exception {

    final StreamCopier streamCopier = new StreamCopier(4096, true);

    final InputStream identityStyleSheetStream =
      getClass().getResourceAsStream("resources/identity.xsl");

    final File identityStyleSheetFile =
      new File(getDirectory(), "identity.xsl");

    streamCopier.copy(identityStyleSheetStream,
                      new FileOutputStream(identityStyleSheetFile));

    final StyleSheetFile styleSheetInputStream =
      new StyleSheetFile(identityStyleSheetFile);

    final ProcessHTTPRecordingWithXSLT processor =
      new ProcessHTTPRecordingWithXSLT(styleSheetInputStream, m_logger);

    final HttpRecordingDocument emptyDocument =
      HttpRecordingDocument.Factory.newInstance();

    processor.process(emptyDocument);

    final String output = m_out.toString();
    AssertUtilities.assertContainsPattern(output,
      "^<\\?xml version=.*\\?>\\s*$");

    verify(m_logger).getOutputLogWriter();

    try {
      styleSheetInputStream.open().read();
      fail("Input stream not closed");
    }
    catch (IOException e) {
    }

    final ProcessHTTPRecordingWithXSLT processor2 =
      new ProcessHTTPRecordingWithXSLT(
        new StyleSheetFile(identityStyleSheetFile), m_logger);

    final HttpRecordingDocument document2 =
      HttpRecordingDocument.Factory.newInstance();
    final HTTPRecordingType recording = document2.addNewHttpRecording();
    recording.addNewMetadata().setVersion("blah");

    processor2.process(document2);

    final String output2 = m_out.toString().substring(output.length());
    AssertUtilities.assertContainsPattern(output2,
      "^<\\?xml version=.*\\?>\\s*" +
      "<http-recording .*?>\\s*" +
      "<metadata>\\s*" +
      "<version>blah</version>\\s*" +
      "</metadata>\\s*" +
      "</http-recording>\\s*$");

    verify(m_logger, times(2)).getOutputLogWriter();
    verifyNoMoreInteractions(m_logger);
  }

  @Test public void testWithStandardTransform() throws Exception {
    final ProcessHTTPRecordingWithXSLT processor =
      new ProcessHTTPRecordingWithXSLT(m_logger);

    final HttpRecordingDocument document =
      HttpRecordingDocument.Factory.newInstance();
    final HTTPRecordingType recording = document.addNewHttpRecording();
    recording.addNewMetadata().setVersion("blah");

    // Will fail with an un-parseable date TransformerException
    processor.process(document);

    final String output = m_out.toString();
    AssertUtilities.assertContains(output, "# blah");

    verify(m_logger).getOutputLogWriter();
    verify(m_logger).error(contains("Unparseable date"));

    // This time it will work.
    recording.addNewMetadata().setTime(Calendar.getInstance());

    final ProcessHTTPRecordingWithXSLT processor2 =
      new ProcessHTTPRecordingWithXSLT(m_logger);

    processor2.process(document);

    verify(m_logger, times(2)).getOutputLogWriter();
    verifyNoMoreInteractions(m_logger);
  }

  @Test public void testWithClojureTransform() throws Exception {
    final ProcessHTTPRecordingWithXSLT processor =
      new ProcessHTTPRecordingWithXSLT(BuiltInStyleSheet.Clojure,
                                       m_logger);

    final HttpRecordingDocument document =
      HttpRecordingDocument.Factory.newInstance();
    final HTTPRecordingType recording = document.addNewHttpRecording();
    recording.addNewMetadata().setVersion("blah");

    recording.addNewMetadata().setTime(Calendar.getInstance());

    processor.process(document);
    verify(m_logger).getOutputLogWriter();
    verifyNoMoreInteractions(m_logger);

    AssertUtilities.assertContains(m_out.toString(), ";; blah");
  }

  @Test public void testWithBadTransform() throws Exception {
    final File badStyleSheetFile = new File(getDirectory(), "bad.xsl");
    badStyleSheetFile.createNewFile();

    final ProcessHTTPRecordingWithXSLT processor =
      new ProcessHTTPRecordingWithXSLT(
        new StyleSheetFile(badStyleSheetFile),
        m_logger);

    final HttpRecordingDocument emptyDocument =
      HttpRecordingDocument.Factory.newInstance();

    // Redirect streams, because XSLTC still chucks some stuff out to stderr.
    new RedirectStandardStreams() {
      protected void runWithRedirectedStreams() throws Exception {
        processor.process(emptyDocument);
    }}.run();

    verify(m_logger, atLeastOnce()).error(isA(String.class));

    // Processor might log multiple messages; ignore.
    // m_loggerStubFactory.assertNoMoreCalls();
  }
}
