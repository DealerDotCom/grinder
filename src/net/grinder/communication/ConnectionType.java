// Copyright (C) 2003 Philip Aston
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

package net.grinder.communication;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Constants that are used to discriminate between different types of
 * connections.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ConnectionType {

  /** Connection type constant. */
  public static final ConnectionType CONTROL = new ConnectionType(0);

  /** Connection type constant. */
  public static final ConnectionType REPORT = new ConnectionType(1);

  /**
   * Serialisation method that reads a ConnectionType from a stream.
   * Package scope.
   *
   * @param in The stream.
   * @return The ConnectionType.
   */
  static ConnectionType read(InputStream in)
    throws CommunicationException {

    try {
      final int b = in.read();

      switch (b) {
      case 0:
        return ConnectionType.CONTROL;

      case 1:
        return ConnectionType.REPORT;

      default:
        throw new CommunicationException("Unknown connection type");
      }
    }
    catch (IOException e) {
      throw new CommunicationException("Failed to read connection type", e);
    }
  }

  private final int m_identity;

  private ConnectionType(int identity) {
    m_identity = identity;
  }

  /**
   * Serialisation method that writes a ConnectionType to a stream.
   * Package scope.
   *
   * @param out The stream.
   * @throws CommunicationException If write failed.
   */
  public void write(OutputStream out) throws CommunicationException {
    try {
      out.write(m_identity);
      out.flush();
    }
    catch (IOException e) {
      throw new CommunicationException("Write failed", e);
    }
  }

  /**
   * Implement {@link Object#hashCode}.
   *
   * @return The hash code.
   */
  public int hashCode() {
    return m_identity;
  }

  /**
   * Equality.
   *
   * @param other An <code>Object</code> to compare.
   * @return <code>true</code> => <code>other</code> is equal to this object.
   */
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }

    if (!(other instanceof ConnectionType)) {
      return false;
    }

    final ConnectionType otherConnectionType = (ConnectionType)other;
    return m_identity == otherConnectionType.m_identity;
  }
}
