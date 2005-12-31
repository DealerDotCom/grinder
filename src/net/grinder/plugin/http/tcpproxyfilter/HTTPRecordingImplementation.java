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

import java.io.IOException;
import java.util.Calendar;

import org.picocontainer.Disposable;

import net.grinder.common.GrinderBuild;
import net.grinder.common.Logger;
import net.grinder.plugin.http.xml.BaseURLType;
import net.grinder.plugin.http.xml.CommonHeadersType;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HttpRecordingDocument;
import net.grinder.plugin.http.xml.RequestType;


/**
 * Contains common state for HTTP recording.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class HTTPRecordingImplementation implements HTTPRecording, Disposable {

  private final HttpRecordingDocument m_recordingDocument =
    HttpRecordingDocument.Factory.newInstance();
  private final Logger m_logger;
  private final HTTPRecordingResultProcessor m_resultProcessor;

  private long m_lastResponseTime = 0;

  /**
   * Constructor.
   *
   * @param resultProcessor
   *          Component which handles result.
   * @param logger
   *          A logger.
   */
  public HTTPRecordingImplementation(
    HTTPRecordingResultProcessor resultProcessor, Logger logger) {

    m_resultProcessor = resultProcessor;
    m_logger = logger;

    final HTTPRecordingType.Metadata httpRecording =
      m_recordingDocument.addNewHttpRecording().addNewMetadata();

    httpRecording.setVersion("The Grinder " + GrinderBuild.getVersionString());
    httpRecording.setTime(Calendar.getInstance());
  }

  /**
   * Add a new request to the recording.
   *
   * @param request The request.
   */
  public void addRequest(RequestType request) {
    synchronized (m_recordingDocument) {
      m_recordingDocument.getHttpRecording().addNewRequest().set(request);
    }
  }

  /**
   * Add a new base URL to the recording.
   *
   * @param baseURL The URL.
   */
  public void addBaseURL(BaseURLType baseURL) {
    synchronized (m_recordingDocument) {
      m_recordingDocument.getHttpRecording().addNewBaseUrl().set(baseURL);
    }
  }

  /**
   * Add new common headers to the recording.
   *
   * @param commonHeaders The headers.
   */
  public void addCommonHeaders(CommonHeadersType commonHeaders) {
    synchronized (m_recordingDocument) {
      m_recordingDocument.getHttpRecording().addNewCommonHeaders()
      .set(commonHeaders);
    }
  }

  /**
   * Called when any response activity is detected. Because the test script
   * represents a single thread of control we need to calculate the sleep deltas
   * using the last time any activity occurred on any connection.
   */
  public void markLastResponseTime() {
    synchronized (this) {
      m_lastResponseTime = System.currentTimeMillis();
    }
  }

  /**
   * Get the last response time.
   *
   * @return The last response time.
   */
  public long getLastResponseTime() {
    synchronized (this) {
      return m_lastResponseTime;
    }
  }

  /**
   * Called after the component has been stopped.
   */
  public void dispose() {
    final HttpRecordingDocument result;

    synchronized (m_recordingDocument) {
      result = (HttpRecordingDocument)m_recordingDocument.copy();
    }

    try {
      m_resultProcessor.process(result);
    }
    catch (IOException e) {
      m_logger.error(e.getMessage());
      e.printStackTrace(m_logger.getErrorLogWriter());
    }
  }
}
