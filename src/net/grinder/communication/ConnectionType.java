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

  /** Number of connection types. */
  static final int NUMBER_OF_CONNECTION_TYPES = 2;

  private final int m_identity;

  private ConnectionType(int identity) {
    m_identity = identity;
  }

  int toInteger() {
    return m_identity;
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
