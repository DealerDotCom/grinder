package net.grinder.plugin.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Active class that accepts a connection on a socket, reads an HTTP request,
 * and returns a response. The details of the request can then be retrieved.
 */
class HTTPRequestHandler implements Runnable {
  private static final Pattern s_contentLengthPattern;

  static {
    try {
      s_contentLengthPattern =
        Pattern.compile("^Content-Length:[ \\t]*(.*)\\r?$",
                        Pattern.MULTILINE |
                        Pattern.CASE_INSENSITIVE);
    }
    catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final ServerSocket m_serverSocket;
  private String m_lastRequestHeaders;
  private byte[] m_lastRequestBody;

  public HTTPRequestHandler() throws Exception {
    m_serverSocket = new ServerSocket(0);
    new Thread(this, getClass().getName()).start();
  }

  public final void shutdown() throws Exception {
    m_serverSocket.close();
  }

  public final String getURL() {
    return "http://localhost:" + m_serverSocket.getLocalPort();
  }

  public final String getLastRequestHeaders() {
    return m_lastRequestHeaders;
  }

  public final byte[] getLastRequestBody() {
    return m_lastRequestBody;
  }

  public final String getRequestFirstHeader() {
    final String text = getLastRequestHeaders();

    final int i = text.indexOf("\r\n");
    TestHTTPRequest.assertTrue("Has at least one line", i>=0);
    return text.substring(0, i);
  }

  public final void assertRequestContainsHeader(String line) {
    final String text = getLastRequestHeaders();

    int start = 0;
    int i;

    while ((i = text.indexOf("\r\n", start)) != -1) {
      if (text.substring(start, i).equals(line)) {
        return;
      }

      start = i + 2;
    }

    if (text.substring(start).equals(line)) {
      return;
    }

    TestHTTPRequest.fail(text + " does not contain " + line);
  }

  public final void assertRequestDoesNotContainHeader(String line) {
    final String text = getLastRequestHeaders();

    int start = 0;
    int i;

    while((i = text.indexOf("\r\n", start)) != -1) {
      TestHTTPRequest.assertTrue(!text.substring(start, i).equals(line));
      start = i + 2;
    }

    TestHTTPRequest.assertTrue(!text.substring(start).equals(line));
  }

  public final void run() {
    try {
      while (true) {
        final Socket localSocket;

        try {
          localSocket = m_serverSocket.accept();
        }
        catch (SocketException e) {
          // Socket's been closed, lets quit.
          break;
        }

        final InputStream in = localSocket.getInputStream();

        final StringBuffer headerBuffer = new StringBuffer();
        final byte[] buffer = new byte[1000];
        int n;
        int bodyStart = -1;

        READ_HEADERS:
        while ((n = in.read(buffer, 0, buffer.length)) != -1) {

          for (int i=0; i<n-3; ++i) {
            if (buffer[i] == '\r' &&
                buffer[i+1] == '\n' &&
                buffer[i+2] == '\r' &&
                buffer[i+3] == '\n') {

              headerBuffer.append(new String(buffer, 0, i));
              bodyStart = i + 4;
              break READ_HEADERS;
            }
          }

          headerBuffer.append(new String(buffer, 0, n));
        }

        if (bodyStart == -1) {
          throw new IOException("No header boundary");
        }

        m_lastRequestHeaders = headerBuffer.toString();

        final Matcher matcher =
          s_contentLengthPattern.matcher(m_lastRequestHeaders);

        if (matcher.find()) {
          final int contentLength =
            Integer.parseInt(matcher.group(1).trim());

          m_lastRequestBody = new byte[contentLength];

          int bodyBytes = n - bodyStart;

          System.arraycopy(buffer, bodyStart, m_lastRequestBody, 0,
                           bodyBytes);

          while (bodyBytes < m_lastRequestBody.length) {
            final int bytesRead =
              in.read(m_lastRequestBody, bodyBytes,
                      m_lastRequestBody.length - bodyBytes);

            if (bytesRead == -1) {
              throw new IOException("Content-length too large");
            }

            bodyBytes += bytesRead;
          }

          if (in.available() > 0) {
            throw new IOException("Content-length too small");
          }
        }
        else {
          m_lastRequestBody = null;
        }

        final OutputStream out = localSocket.getOutputStream();

        final StringBuffer response = new StringBuffer();
        writeHeaders(response);
        response.append("\r\n");
        out.write(response.toString().getBytes());
        out.flush();

        localSocket.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {
        m_serverSocket.close();
      }
      catch (IOException e) {
        // Whatever.
      }
    }
  }

  /**
   * Subclass HTTPRequestHandler to change these default headers.
   */
  protected void writeHeaders(StringBuffer response) {
    response.append("HTTP/1.0 200 OK\r\n");
  }
}