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
 * Class that represents the endpoint of a TCP connection.
 *
 * @author <a href="mailto:paston@bea.com">Philip Aston</a>
 * @version $Revision$
 */
public final class EndPoint {

  private final String m_host;
  private final int m_port;
  private final int m_hashCode;

  /**
   * Constructor.
   *
   * @param host Host name or IP address.
   * @param port Port.
   */
  public EndPoint(String host, int port) {

    m_host = host.toLowerCase();
    m_port = port;

    m_hashCode = m_host.hashCode() ^ m_port;
  }

  /**
   * Accessor.
   *
   * @return Host name or IP address.
   */
  public String getHost() {
    return m_host;
  }

  /**
   * Accessor.
   *
   * @return an <code>int</code> value
   */
  public int getPort() {
    return m_port;
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

    if (!(other instanceof EndPoint)) {
      return false;
    }

    final EndPoint otherEndPoint = (EndPoint)other;

    return
      hashCode() == otherEndPoint.hashCode() &&
      getPort() == otherEndPoint.getPort() &&
      getHost().equals(otherEndPoint.getHost());
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
   * String representation.
   *
   * @return The string.
   */
  public String toString()  {
    return m_host + ":" + m_port;
  }
}
