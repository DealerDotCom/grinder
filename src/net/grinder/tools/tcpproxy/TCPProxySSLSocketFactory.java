// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;


/**
 * {@link TCPProxySocketFactory} for SSL connections.
 *
 * <p>The JSSE docs rabbit on about being able to create factories
 * with the required parameters, this is a lie. Where is
 * "SSL[Server]SocketFactory.setEnabledCipherSuites()"? Hence the need
 * for our own abstract factories.</p>
 *
 * @author Philip Aston
 * @author Phil Dawes
 * @version $Revision$
 */
public final class TCPProxySSLSocketFactory implements TCPProxySocketFactory {

  private final ServerSocketFactory m_serverSocketFactory;
  private final SocketFactory m_clientSocketFactory;

  /**
   * ARGHH. I hate JSSE.
   *
   * <p>The JSSE docs rabbit on about being able to create factories
   * with the required parameters, this is a lie. Where is
   * "SSL[Server]SocketFactory.setEnabledCipherSuites()"? Hence the
   * need for our own abstract factories.</p>
   *
   * <p>We can't install our own TrustManagerFactory without messing
   * with the security properties file. Hence we create our own
   * SSLContext and initialise it. Passing null as the keystore
   * parameter to SSLContext.init() results in a empty keystore
   * being used, as does passing the key manager array obtain from
   * keyManagerFactory.getInstance().getKeyManagers(). To pick up
   * the "default" keystore system properties, we have to read them
   * explicitly. UGLY, but necessary so we understand the expected
   * properties.</p>
   *
   * - PhilA
   *
   * @exception IOException If an I/O error occurs.
   * @exception GeneralSecurityException If a security error occurs.
   */
  public TCPProxySSLSocketFactory()
    throws IOException, GeneralSecurityException {

    final SSLContext sslContext = SSLContext.getInstance("SSL");

    final KeyManagerFactory keyManagerFactory =
      KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

    final String keyStoreFile =
      System.getProperty(JSSEConstants.KEYSTORE_PROPERTY);
    final char[] keyStorePassword =
      System.getProperty(JSSEConstants.KEYSTORE_PASSWORD_PROPERTY, "")
      .toCharArray();
    final String keyStoreType =
      System.getProperty(JSSEConstants.KEYSTORE_TYPE_PROPERTY, "jks");

    final KeyStore keyStore;

    if (keyStoreFile != null) {
      keyStore = KeyStore.getInstance(keyStoreType);
      keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword);
    }
    else {
      keyStore = null;
    }

    keyManagerFactory.init(keyStore, keyStorePassword);

    sslContext.init(keyManagerFactory.getKeyManagers(),
		    new TrustManager[] { new TrustEveryone() },
		    null);

    m_clientSocketFactory = sslContext.getSocketFactory();
    m_serverSocketFactory = sslContext.getServerSocketFactory();
  }

  /**
   * Factory method for server sockets.
   *
   * @param localHost Local host.
   * @param localPort Local port.
   * @param timeout Socket timeout.
   * @return A new <code>ServerSocket</code>.
   * @exception IOException If an error occurs.
   */
  public final ServerSocket createServerSocket(String localHost,
					       int localPort,
					       int timeout)
    throws IOException {

    final SSLServerSocket socket =
      (SSLServerSocket)m_serverSocketFactory.createServerSocket(
	localPort, 50, InetAddress.getByName(localHost));

    socket.setSoTimeout(timeout);

    socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

    return socket;
  }

  /**
   * Factory method for client sockets.
   *
   * @param remoteHost Remote host.
   * @param remotePort Remote port.
   * @return A new <code>Socket</code>.
   * @exception IOException If an error occurs.
   */
  public final Socket createClientSocket(String remoteHost, int remotePort)
    throws IOException {

    final SSLSocket socket =
      (SSLSocket)m_clientSocketFactory.createSocket(remoteHost, remotePort);

    socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
    return socket;
  }

  /**
   * For the purposes of sniffing, we don't care whether the cert
   * chain is trusted or not, so here's an implementation which
   * accepts everything -PD
   */
  private static class TrustEveryone implements X509TrustManager {

    public boolean isClientTrusted (X509Certificate[] chain) {
      return true;
    }

    public boolean isServerTrusted (X509Certificate[] chain) {
      return true;
    }

    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
      return null;
    }
  }
}

