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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.picocontainer.Disposable;

import net.grinder.common.GrinderBuild;
import net.grinder.common.Logger;
import net.grinder.plugin.http.xml.BaseURLType;
import net.grinder.plugin.http.xml.CommonHeadersType;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HeaderType;
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

    // Extract default headers that are present in all common headers.
    final CommonHeadersType[] commonHeaders =
      result.getHttpRecording().getCommonHeadersArray();

    final Map defaultHeaders = new HashMap();
    final Set notDefaultHeaders = new HashSet();

    for (int i = 0; i < commonHeaders.length; ++i) {
      final HeaderType[] headers = commonHeaders[i].getHeaderArray();

      for (int j = 0; j < headers.length; ++j) {
        final String name = headers[j].getName();
        final String value = headers[j].getValue();

        if (notDefaultHeaders.contains(name)) {
          continue;
        }

        final String existing = (String)defaultHeaders.put(name, value);

        if (existing != null && !value.equals(existing)) {
          defaultHeaders.remove(name);
          notDefaultHeaders.add(name);
        }
      }
    }

    if (defaultHeaders.size() > 0) {
      final CommonHeadersType[] newCommonHeaders =
        new CommonHeadersType[commonHeaders.length + 1];

      System.arraycopy(
        commonHeaders, 0, newCommonHeaders, 1, commonHeaders.length);

      final String defaultHeadersID = "defaultHeaders";
      newCommonHeaders[0] = CommonHeadersType.Factory.newInstance();
      newCommonHeaders[0].setHeadersId(defaultHeadersID);

      final Iterator iterator = defaultHeaders.entrySet().iterator();
      while (iterator.hasNext()) {
        final Entry entry = (Entry)iterator.next();
        final HeaderType header = newCommonHeaders[0].addNewHeader();
        header.setName((String)entry.getKey());
        header.setValue((String)entry.getValue());
      }

      for (int i = 0; i < commonHeaders.length; ++i) {
        final HeaderType[] headers = commonHeaders[i].getHeaderArray();
        for (int j = headers.length - 1; j >= 0; --j) {
          if (defaultHeaders.containsKey(headers[j].getName())) {
            commonHeaders[i].removeHeader(j);
          }
        }

        commonHeaders[i].setExtends(defaultHeadersID);
      }

      result.getHttpRecording().setCommonHeadersArray(newCommonHeaders);
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
