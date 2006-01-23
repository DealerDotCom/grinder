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
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.xmlbeans.XmlObject;
import org.picocontainer.Disposable;

import net.grinder.common.GrinderBuild;
import net.grinder.common.Logger;
import net.grinder.plugin.http.xml.BaseURIType;
import net.grinder.plugin.http.xml.CommonHeadersType;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HeaderType;
import net.grinder.plugin.http.xml.HeadersType;
import net.grinder.plugin.http.xml.HttpRecordingDocument;
import net.grinder.plugin.http.xml.PageType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;


/**
 * Contains common state for HTTP recording.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class HTTPRecordingImplementation implements HTTPRecording, Disposable {

  /**
   * Headers which are likely to have common values.
   */
  private static final Set COMMON_HEADERS = new HashSet(Arrays.asList(
    new String[] {
      "Accept",
      "Accept-Charset",
      "Accept-Encoding",
      "Accept-Language",
      "Cache-Control",
      "Referer", // Deliberate misspelling to match specification.
      "User-Agent",
    }
  ));

  private final HttpRecordingDocument m_recordingDocument =
    HttpRecordingDocument.Factory.newInstance();
  private final Logger m_logger;
  private final HTTPRecordingResultProcessor m_resultProcessor;

  private final IntGenerator m_requestIDGenerator = new IntGenerator();
  private final BaseURLMap m_baseURLMap = new BaseURLMap();
  private final CommonHeadersMap m_commonHeadersMap = new CommonHeadersMap();
  private final PageMap m_pageMap = new PageMap();

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
   * <p>
   * The "global information" (request ID, base URL ID, common headers, page
   * etc.) is filled in by this method.
   * </p>
   *
   * @param connectionDetails
   *          The connection used to make the request.
   * @param request
   *          The request as a disconnected element.
   */
  public void addRequest(ConnectionDetails connectionDetails,
                         RequestType request) {

    request.setRequestId("request" + m_requestIDGenerator.next());

    final BaseURIType baseURL =
      m_baseURLMap.getBaseURL(
        connectionDetails.isSecure() ?
          BaseURIType.Scheme.HTTPS : BaseURIType.Scheme.HTTP,
        connectionDetails.getRemoteEndPoint());

    request.getUri().setExtends(baseURL.getUriId());

    m_commonHeadersMap.extractCommonHeaders(request);

    final PageType pageType = m_pageMap.getPage(baseURL, request);

    synchronized (pageType) {
      pageType.addNewRequest().set(request);
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

        if (existing != null && !value.equals(existing) ||
            existing == null && i > 0) {
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

  private final class BaseURLMap {
    private Map m_map = new HashMap();
    private IntGenerator m_idGenerator = new IntGenerator();

    public BaseURIType getBaseURL(
      BaseURIType.Scheme.Enum scheme, EndPoint endPoint) {

      final Object key = scheme.toString() + "://" + endPoint;

      synchronized (m_map) {
        final BaseURIType existing = (BaseURIType)m_map.get(key);

        if (existing != null) {
          return existing;
        }

        final BaseURIType result;

        synchronized (m_recordingDocument) {
          result = m_recordingDocument.getHttpRecording().addNewBaseUri();
        }

        result.setUriId("url" + m_idGenerator.next());
        result.setScheme(scheme);
        result.setHost(endPoint.getHost());
        result.setPort(endPoint.getPort());

        m_map.put(key, result);

        return result;
      }
    }
  }

  private final class CommonHeadersMap {
    private Map m_map = new HashMap();
    private IntGenerator m_idGenerator = new IntGenerator();

    public void extractCommonHeaders(RequestType request) {

      final CommonHeadersType commonHeaders =
        CommonHeadersType.Factory.newInstance();
      final HeadersType newRequestHeaders = HeadersType.Factory.newInstance();

      final XmlObject[] children = request.getHeaders().selectPath("./*");

      for (int i = 0; i < children.length; ++i) {
        if (children[i] instanceof HeaderType) {
          final HeaderType header = (HeaderType)children[i];

          if (COMMON_HEADERS.contains(header.getName())) {
            commonHeaders.addNewHeader().set(header);
          }
          else {
            newRequestHeaders.addNewHeader().set(header);
          }
        }
        else {
          newRequestHeaders.addNewAuthorization().set(children[i]);
        }
      }

      // Key that ignores ID.
      final Object key =
        Arrays.asList(commonHeaders.getHeaderArray()).toString();

      synchronized (m_map) {
        final CommonHeadersType existing = (CommonHeadersType)m_map.get(key);

        if (existing != null) {
          newRequestHeaders.setExtends(existing.getHeadersId());
        }
        else {
          commonHeaders.setHeadersId("headers" + m_idGenerator.next());

          synchronized (m_recordingDocument) {
            m_recordingDocument.getHttpRecording().addNewCommonHeaders()
            .set(commonHeaders);
          }

          m_map.put(key, commonHeaders);

          newRequestHeaders.setExtends(commonHeaders.getHeadersId());
        }
      }

      request.setHeaders(newRequestHeaders);
    }
  }

  private final class PageMap {
    private final Pattern m_isPageResourcePathPattern;
    private final Map m_map = new HashMap();
    private final IntGenerator m_idGenerator = new IntGenerator();

    private PageMap() {
      m_isPageResourcePathPattern = Pattern.compile(
        ".*(\\.css|\\.gif|\\.ico|\\.jpe?g|\\.js)\\b.*",
        Pattern.CASE_INSENSITIVE);
    }

    public PageType getPage(BaseURIType baseURL, RequestType request) {

      synchronized (m_map) {
        final PageType existing = (PageType) m_map.get(baseURL);

        // Crude heuristic to figure out whether request is the start of
        // a new page or not.
        if (existing != null && !request.isSetBody()) {
          final String pathToCheck;
          final String unparsedPath = request.getUri().getUnparsed();

          if (unparsedPath != null) {
            pathToCheck = unparsedPath;
          }
          else {
            final String[] segmentArray =
              request.getUri().getParsed().getSegmentArray();
            pathToCheck = segmentArray[segmentArray.length - 1];
          }

          if (m_isPageResourcePathPattern.matcher(pathToCheck).matches()) {
            return existing;
          }
        }

        final PageType result;

        synchronized (m_recordingDocument) {
          result = m_recordingDocument.getHttpRecording().addNewPage();
        }

        result.setPageId("page" + m_idGenerator.next());

        m_map.put(baseURL, result);

        return result;
      }
    }
  }
}
