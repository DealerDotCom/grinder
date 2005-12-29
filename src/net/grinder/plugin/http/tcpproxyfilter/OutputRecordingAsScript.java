// Copyright (C) 2005 Philip Aston
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

package net.grinder.plugin.http.tcpproxyfilter;

import java.io.InputStream;
import java.io.PrintWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.grinder.common.Logger;
import net.grinder.plugin.http.xml.HttpRecordingDocument;
import net.grinder.util.SimpleLogger;


/**
 * Output an {@link HTTPRecordingImplementation} result as XML text.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class OutputRecordingAsScript implements HTTPRecordingResultProcessor {

  private final TransformerFactory m_transformerFactory =
    TransformerFactory.newInstance();
  private final Logger m_logger;

  /**
   * Constructor.
   *
   * @param logger Where to direct the output.
   */
  public OutputRecordingAsScript(Logger logger) {
    m_logger = logger;
  }

  /**
   * Produce output.
   *
   * @param result The result to process.
   */
  public void process(HttpRecordingDocument result) {

    final String styleSheet = System.getProperty(
      "transformHTTPRecordingToScript",
      "resources/httpRecordingToScript.xsl");

    final InputStream styleSheetInputStream =
      getClass().getResourceAsStream(styleSheet);

    if (styleSheetInputStream == null) {
      m_logger.error(
        "Could not locate XSLT transform '" + styleSheet + "' in classpath");
      return;
    }

    try {
      final Transformer transformer =
        m_transformerFactory.newTransformer(
          new StreamSource(styleSheetInputStream));

      final PrintWriter outputWriter = m_logger.getOutputLogWriter();

      transformer.transform(new DOMSource(result.getDomNode()),
                            new StreamResult(outputWriter));

      outputWriter.println();
    }
    catch (TransformerConfigurationException e) {
      m_logger.error("Failed to initialise XSLT transform");
      e.printStackTrace(m_logger.getErrorLogWriter());
    }
    catch (TransformerException e) {
      m_logger.error("XSLT transformation failed");
      e.printStackTrace(m_logger.getErrorLogWriter());
    }

    /*
    if (properties != null) {
      // Set an XSLT parameter for each domain parameter.
      final Iterator parameterIterator = properties.entrySet().iterator();

      while (parameterIterator.hasNext()) {
    final Map.Entry entry = (Map.Entry)parameterIterator.next();
        transformer.setParameter((String)entry.getKey(), entry.getValue());
      }
    }
    */
  }

  public static void main(String[] arguments) throws Exception {
    final HttpRecordingDocument recording =
      HttpRecordingDocument.Factory.parse(
        OutputRecordingAsScript.class.getResourceAsStream(
          "resources/recording.xml"));

    final OutputRecordingAsScript processor =
      new OutputRecordingAsScript(
        new SimpleLogger("test",
                         new PrintWriter(System.out),
                         new PrintWriter(System.err)));

    processor.process(recording);
  }
}
