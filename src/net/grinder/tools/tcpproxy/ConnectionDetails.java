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


/**
 * Class that represents a TCP connection.
 *
 * @author <a href="mailto:paston@bea.com">Philip Aston</a>
 * @version $Revision$
 */
public final class ConnectionDetails {
  private final int m_hashCode;

  private final EndPoint m_localEndPoint;
  private final EndPoint m_remoteEndPoint;
  private final boolean m_isSecure;
  private final String m_connectionIdentity;

  /**
   * Creates a new <code>ConnectionDetails</code> instance.
   *
   * @param localHost a <code>String</code> value
   * @param localPort an <code>int</code> value
   * @param remoteHost a <code>String</code> value
   * @param remotePort an <code>int</code> value
   * @param isSecure a <code>boolean</code> value
   * @throws IllegalArgumentException If local and remote details are the same.
   */
  public ConnectionDetails(String localHost, int localPort, String remoteHost,
                           int remotePort, boolean isSecure) {

    m_localEndPoint = new EndPoint(localHost, localPort);
    m_remoteEndPoint = new EndPoint(remoteHost, remotePort);
    m_isSecure = isSecure;

    m_hashCode =
      m_localEndPoint.hashCode() ^
      m_remoteEndPoint.hashCode() ^
      (m_isSecure ? 0x55555555 : 0);

    final int c = localHost.compareTo(remoteHost);
    final boolean localIsLeast;

    if (c == 0) {
      if (localPort == remotePort) {
        throw new IllegalArgumentException(
          "Local and remote sockets are the same");
      }

      localIsLeast = localPort < remotePort;
    }
    else {
      localIsLeast = c < 0;
    }

    if (localIsLeast) {
      m_connectionIdentity =
        localHost + "|" + localPort + "|" +
        remoteHost + "|" + remotePort + "|" +
        isSecure;
    }
    else {
      m_connectionIdentity =
        remoteHost + "|" + remotePort + "|" +
        localHost + "|" + localPort + "|" +
        isSecure;
    }
  }

  /**
   * String representation of the connection.
   *
   * @return a <code>String</code> value
   */
  public String toString() {
    return m_localEndPoint + "->" + m_remoteEndPoint;
  }

  /**
   * Describe <code>getURLBase</code> method here.
   *
   * @param protocol a <code>String</code> value
   * @return a <code>String</code> value
   */
  public String getURLBase(String protocol) {

    // Hackety do dah..
    return protocol + (m_isSecure ? "s://" : "://") + m_remoteEndPoint;
  }

  /**
   * Accessor.
   *
   * @return a <code>boolean</code> value
   */
  public boolean isSecure() {
    return m_isSecure;
  }

  /**
   * Accessor.
   *
   * @return a <code>String</code> value
   */
  public EndPoint getRemoteEndPoint() {
    return m_remoteEndPoint;
  }

  /**
   * Accessor.
   *
   * @return a <code>String</code> value
   */
  public String getRemoteHost() {
    return m_remoteEndPoint.getHost();
  }

  /**
   * Accessor.
   *
   * @return an <code>int</code> value
   */
  public int getRemotePort() {
    return m_remoteEndPoint.getPort();
  }

  /**
   * Accessor.
   *
   * @return a <code>String</code> value
   */
  public EndPoint getLocalEndPoint() {
    return m_localEndPoint;
  }

  /**
   * Accessor.
   *
   * @return a <code>String</code> value
   */
  public String getLocalHost() {
    return m_localEndPoint.getHost();
  }

  /**
   * Accessor.
   *
   * @return an <code>int</code> value
   */
  public int getLocalPort() {
    return m_localEndPoint.getPort();
  }

  /**
   * Value based equality.
   *
   * @param other an <code>Object</code> value
   * @return <code>true</code> => <code>other</code> is equal to this object.
   */
  public boolean equals(Object other) {

    if (other == this) {
      return true;
    }

    if (!(other instanceof ConnectionDetails)) {
      return false;
    }

    final ConnectionDetails otherConnectionDetails =
      (ConnectionDetails)other;

    return
      hashCode() == otherConnectionDetails.hashCode() &&
      isSecure() == otherConnectionDetails.isSecure() &&
      getLocalEndPoint().equals(otherConnectionDetails.getLocalEndPoint()) &&
      getRemoteEndPoint().equals(otherConnectionDetails.getRemoteEndPoint());
  }

  /**
   * Implement {@link Object#hashCode}.
   *
   * @return The hash code.
   */
  public int hashCode() {
    return m_hashCode;
  }

  /**
   * Return a <code>String</code> that represents the connection.
   * <code>ConnectionDetails</code> representing either end of the
   * same connection always return the same thing.
   *
   * @return Represents the connection.
   */
  public String getConnectionIdentity() {
    return m_connectionIdentity;
  }

  /**
   * Return a <code>ConnectionDetails</code> representing the other
   * end of the connection.
   *
   * @return The other end of the connection to this.
   */
  public ConnectionDetails getOtherEnd() {

    return new ConnectionDetails(getRemoteHost(), getRemotePort(),
                                 getLocalHost(), getLocalPort(),
                                 isSecure());
  }
}
