// Copyright (C) 2011 Philip Aston
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

package net.grinder.synchronisation;

import java.util.HashMap;
import java.util.Map;

import net.grinder.synchronisation.BarrierGroup.BarrierIdentity;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentityGenerator;
import clover.retrotranslator.edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;


/**
 * {@link BarrierGroups} implementation for use within the local JVM.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class LocalBarrierGroups implements BarrierGroups {

  // Currently the extant barrier groups grow without limit - each new group
  // name used by getGroup() will add a new entry to the map. To avoid this
  // memory leak, we would need to introduce a BarrierGroup.destroy() protocol,
  // perhaps based on the garbage collection of barrier instances. Ignoring
  // for now.
  // Guarded by this.
  private final Map<String, BarrierGroup> m_groups =
    new HashMap<String, BarrierGroup>();

  private final BarrierIdentityGenerator m_identityGenerator =
    new LocalIdentityGenerator();

  /**
   * {@inheritDoc}
   */
  public BarrierGroup getGroup(String name) {
    synchronized (m_groups) {
      final BarrierGroup existing = m_groups.get(name);

      if (existing != null) {
        return existing;
      }

      final BarrierGroup newInstance = new BarrierGroupImplementation(name);
      m_groups.put(name, newInstance);

      return newInstance;
    }
  }

  /**
   * {@inheritDoc}
   */
  public BarrierIdentityGenerator getIdentityGenerator() {
    return m_identityGenerator;
  }

  private class LocalIdentityGenerator implements BarrierIdentityGenerator {

    private final AtomicInteger m_next = new AtomicInteger();

    public BarrierIdentity next() {
      return new LocalBarrierIdentity(m_next.getAndIncrement());
    }
  }

  private static final class LocalBarrierIdentity implements BarrierIdentity {

    private final int m_value;

    public LocalBarrierIdentity(int value) {
      m_value = value;
    }

    @Override public int hashCode() {
      return m_value;
    }

    @Override public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      return m_value == ((LocalBarrierIdentity) o).m_value;
    }
  }
}
