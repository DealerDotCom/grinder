// Copyright (C) 2008 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.util;

import java.util.HashMap;
import java.util.Map;


/**
 * Implementation of @{link {@link AllocateLowestNumber}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public final class AllocateLowestNumberImplementation
  implements AllocateLowestNumber {

  private static final long serialVersionUID = 1L;

  /** Guarded by self. */
  private Map m_map = new HashMap();

  /** Guarded by {@link #m_map}. */
  private int m_nextN = 0;

  /**
   * Add a new object. If the object already belongs to the set, the existing
   * associated number is returned.
   *
   * @param o
   *            The object.
   * @return The associated number.
   */
  public int add(Object o) {
    synchronized (m_map) {
      final Integer n = (Integer) m_map.get(o);

      if (n != null) {
        return n.intValue();
      }

      final int nextN = m_nextN;

      m_map.put(o, new Integer(nextN));

      ++m_nextN;

      final int mapSize = m_map.size();

      while (m_nextN < mapSize && m_map.containsValue(new Integer(m_nextN))) {
        ++m_nextN;
      }

      return nextN;
    }
  }

  /**
   * Remove an object from the set. The number previously associated with
   * the object (if any) is freed for re-use.
   *
   * @param o The object.
   */
  public void remove(Object o) {
    synchronized (m_map) {
      final Integer n = (Integer) m_map.remove(o);

      if (n != null) {
        if (n.intValue() <= m_nextN) {
          m_nextN = n.intValue();
        }
      }
    }
  }

  /**
   * Get the number associated with the given object.
   *
   * @param o The object.
   * @return The number.
   * @throws NoSuchObjectException if the obejct could not be found.
   */
  public int get(Object o) throws NoSuchObjectException {
    final Integer result;
    synchronized (m_map) {
      result = (Integer)m_map.get(o);
    }

    if (result == null) {
      throw new NoSuchObjectException("Could not find '" + o);
    }

    return result.intValue();
  }
}
