// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
// Copyright (C) 2003 Bertrand Ave
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

package net.grinder.tools.tcpproxy;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import net.grinder.util.TerminalColour;


/**
 * Base class for TCPProxyEngine implementations.
 *
 * @author Phil Dawes
 * @author Philip Aston
 * @author Bertrand Ave
 * @version $Revision$
 */
public abstract class AbstractTCPProxyEngine implements TCPProxyEngine {

  private final TCPProxyFilter m_requestFilter;
  private final TCPProxyFilter m_responseFilter;
  private final String m_localHost;
  private final String m_requestColour;
  private final String m_responseColour;

  private final PrintWriter m_outputWriter;

  private final TCPProxySocketFactory m_socketFactory;
  private final ServerSocket m_serverSocket;

  /**
   * Constructor.
   *
   * @param socketFactory Factory for plain old sockets.
   * @param requestFilter Request filter.
   * @param responseFilter Response filter.
   * @param outputWriter Writer to terminal.
   * @param localHost Local host name.
   * @param localPort Local port.
   * @param useColour Whether to use colour.
   * @param timeout Timeout in milliseconds.
   *
   * @exception IOException If an I/O error occurs.
   */
  public AbstractTCPProxyEngine(TCPProxySocketFactory socketFactory,
                                TCPProxyFilter requestFilter,
                                TCPProxyFilter responseFilter,
                                PrintWriter outputWriter,
                                String localHost,
                                int localPort,
                                boolean useColour,
                                int timeout)
    throws IOException {

    m_outputWriter = outputWriter;

    m_socketFactory = socketFactory;
    m_requestFilter = requestFilter;
    m_responseFilter = responseFilter;
    m_localHost = localHost;

    if (useColour) {
      m_requestColour = TerminalColour.RED;
      m_responseColour = TerminalColour.BLUE;
    }
    else {
      m_requestColour = "";
      m_responseColour = "";
    }

    m_serverSocket =
      m_socketFactory.createServerSocket(localHost, localPort, timeout);
  }

  /**
   * Stop the engine and flush filter buffer.
   */
  public void stop() {

    m_requestFilter.stop();
    m_responseFilter.stop();

    // Close socket to stop engine.
    try {
      getServerSocket().close();
    }
    catch (java.io.IOException ioe) {
      // Be silent.
    }
  }

  /**
   * Main event loop.
   */
  public abstract void run();

  /**
   * Accessor for server socket.
   *
   * @return The <code>ServerSocket</code> socket.
   */
  public final ServerSocket getServerSocket() {
    return m_serverSocket;
  }

  /**
   * Allow subclasses to access socket factory.
   *
   * @return The socket factory.
   */
  protected final TCPProxySocketFactory getSocketFactory() {
    return m_socketFactory;
  }

  /**
   * Allow subclasses to access details of the local host.
   *
   * @return The local host.
   */
  protected final String getLocalHost() {
    return m_localHost;
  }

  /**
   * Allow subclasses to access request filter.
   *
   * @return The filter.
   */
  protected final TCPProxyFilter getRequestFilter() {
    return m_requestFilter;
  }

  /**
   * Allow subclasses to access response filter.
   *
   * @return The filter.
   */
  protected final TCPProxyFilter getResponseFilter() {
    return m_responseFilter;
  }

  /**
   * Allow subclasses to access colour terminal control code for
   * request streams.
   *
   * @return The filter.
   */
  protected final String getRequestColour() {
    return m_requestColour;
  }

  /**
   * Allow subclasses to access request terminal control code for
   * response streams.
   *
   * @return The filter.
   */
  protected final String getResponseColour() {
    return m_responseColour;
  }

  /**
   * Launch a pair of threads to handle bi-directional stream
   * communication.
   *
   * @param localSocket Local socket.
   * @param localInputStream Input stream.
   * @param localOutputStream Output stream.
   * @param remoteHost Remote host name.
   * @param remotePort Remote host port.
   * @param isSecure Whether the connection is secure.
   * @exception IOException If an I/O error occurs.
   */
  protected final void launchThreadPair(Socket localSocket,
                                        InputStream localInputStream,
                                        OutputStream localOutputStream,
                                        String remoteHost,
                                        int remotePort,
                                        boolean isSecure)
    throws IOException {

    final Socket remoteSocket =
      m_socketFactory.createClientSocket(remoteHost, remotePort);

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(getLocalHost(),
                            localSocket.getPort(),
                            remoteHost,
                            remoteSocket.getPort(),
                            isSecure);

    new FilteredStreamThread(localInputStream,
                             new OutputStreamFilterTee(
                               connectionDetails,
                               remoteSocket.getOutputStream(),
                               m_requestFilter,
                               m_requestColour));

    new FilteredStreamThread(remoteSocket.getInputStream(),
                             new OutputStreamFilterTee(
                               connectionDetails.getOtherEnd(),
                               localOutputStream,
                               m_responseFilter,
                               m_responseColour));
  }


  /**
   * <code>Runnable</code> which actively reads an input stream and
   * writes to an output stream, passing the data through a filter.
   */
  protected final class FilteredStreamThread implements Runnable {

    // For simplicity, the filters take a buffer oriented approach.
    // This means that they all break at buffer boundaries. Our buffer
    // is huge, so we shouldn't practically cause a problem, but the
    // network clearly can by giving us message fragments. I consider
    // this a bug, we really ought to take a stream oriented approach.
    private static final int BUFFER_SIZE = 65536;

    private final InputStream m_in;
    private final OutputStreamFilterTee m_outputStreamFilterTee;

    /**
     * Constructor.
     */
    FilteredStreamThread(InputStream in,
                         OutputStreamFilterTee outputStreamFilterTee) {

      m_in = in;
      m_outputStreamFilterTee = outputStreamFilterTee;

      new Thread(this,
                 "Filter thread for " +
                 outputStreamFilterTee.getConnectionDetails().getDescription())
        .start();
    }

    /**
     * Main event loop.
     */
    public void run() {

      m_outputStreamFilterTee.connectionOpened();

      final byte[] buffer = new byte[BUFFER_SIZE];

      try {
        while (true) {
          final int bytesRead = m_in.read(buffer, 0, BUFFER_SIZE);

          if (bytesRead == -1) {
            break;
          }

          m_outputStreamFilterTee.handle(buffer, bytesRead);
        }
      }
      catch (IOException e) {
        // Most likely SocketException("socket closed") or
        // IOException("Stream closed"). Ignore.
      }
      finally {
        m_outputStreamFilterTee.connectionClosed();
      }

      // Tidy up.
      try {
        m_in.close();
      }
      catch (IOException e) {
        // Ignore.
      }
    }
  }

  /**
   * Filter like class that delegates to a user filter and tees the
   * result to an output stream. It is constructed for a particular
   * connection. Also performs logging to the terminal.
   */
  protected final class OutputStreamFilterTee {

    private final ConnectionDetails m_connectionDetails;
    private final OutputStream m_out;
    private final TCPProxyFilter m_filter;
    private final String m_colour;
    private final String m_resetColour;

    /**
     * Constructor.
     *
     * @param connectionDetails Connection details.
     * @param out The output stream.
     * @param filter The user filter.
     * @param colourString Terminal control code which sets appropriate
     * colours for this stream.
     */
    public OutputStreamFilterTee(ConnectionDetails connectionDetails,
                                 OutputStream out, TCPProxyFilter filter,
                                 String colourString) {

      m_connectionDetails = connectionDetails;
      m_out = out;
      m_filter = filter;
      m_colour = colourString;
      m_resetColour = m_colour.length() > 0 ? TerminalColour.NONE : "";
    }

    /**
     * A new connection has been opened.
     */
    public void connectionOpened() {

      preOutput();

      try {
        m_filter.connectionOpened(m_connectionDetails);
      }
      catch (Exception e) {
        e.printStackTrace(System.err);
      }
      finally {
        postOutput();
      }
    }

    /**
     * Handle a message fragment.
     *
     * @param buffer Contains the data.
     * @param bytesRead How many bytes of data in <code>buffer</code>.
     * @exception IOException If an I/O error occurs writing to the
     * output stream.
     */
    public void handle(byte[] buffer, int bytesRead) throws IOException {

      preOutput();

      byte[] newBytes = null;

      try {
        newBytes = m_filter.handle(m_connectionDetails, buffer, bytesRead);
      }
      catch (Exception e) {
        e.printStackTrace(System.err);
      }
      finally {
        postOutput();
      }

      if (newBytes != null) {
        m_out.write(newBytes);
      }
      else {
        m_out.write(buffer, 0, bytesRead);
      }
    }

    /**
     * A connection has been closed.
     */
    public void connectionClosed() {

      preOutput();

      try {
        m_filter.connectionClosed(m_connectionDetails);
      }
      catch (Exception e) {
        e.printStackTrace(System.err);
      }
      finally {
        postOutput();
      }

      // Close our output stream. This will cause any
      // FilteredStreamThread managing the paired stream to exit.
      try {
        m_out.close();
      }
      catch (IOException e) {
        // Ignore.
      }
    }

    /**
     * Accessor for connection details.
     *
     * @return The connection details.
     */
    public ConnectionDetails getConnectionDetails() {
      return m_connectionDetails;
    }

    private void preOutput() {
      m_outputWriter.print(m_colour);
    }

    private void postOutput() {
      m_outputWriter.print(m_resetColour);
      m_outputWriter.flush();
    }
  }
}

