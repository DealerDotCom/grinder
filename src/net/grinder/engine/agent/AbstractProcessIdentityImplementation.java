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

package net.grinder.engine.agent;

import java.io.Serializable;


/**
 * Common process identity implementation.
 *
 * @author Philip Aston
 * @version $Revision$
 */
abstract class AbstractProcessIdentityImplementation implements Serializable {

  private final String m_identity;
  private String m_name;

  protected AbstractProcessIdentityImplementation(String identity,
                                                  String name) {
    m_identity = identity;
    m_name = name;
  }

  /**
   * Return the process name.
   *
   * @return The process name.
   */
  public final String getName() {
    return m_name;
  }

  /**
   * Allows the public process name to be changed.
   *
   * @param The new process name.
   */
  public void setName(String name) {
    m_name = name;
  }

  /**
   * Implement equality semantics. We compare equal to all copies of
   * ourself, but nothing else.
   *
   * @return The hash code.
   */
  public final int hashCode() {
    return m_identity.hashCode();
  }

  /**
   * Implement equality semantics. We compare equal to all copies of
   * ourself, but nothing else.
   *
   * @param o Object to compare.
   * @return <code>true</code> => its equal.
   */
  public final boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof AbstractProcessIdentityImplementation)) {
      return false;
    }

    final String otherIdentity =
      ((AbstractProcessIdentityImplementation)o).m_identity;

    return
      m_identity.equals(otherIdentity) &&
      getClass().equals(o.getClass());
  }

  /**
   * String representation.
   *
   * @return A string representation of this process identity.
   */
  public final String toString() {
    return "Process '" + m_name + "' [" + m_identity + "]";
  }
}
