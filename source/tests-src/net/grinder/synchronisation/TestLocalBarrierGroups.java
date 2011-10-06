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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import net.grinder.synchronisation.BarrierGroup.Listener;
import net.grinder.synchronisation.messages.BarrierIdentity;

import org.junit.Before;
import org.junit.Test;


/**
 * Unit tests for {@link LocalBarrierGroups}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestLocalBarrierGroups {

  private static final BarrierIdentity ID1 = new BarrierIdentity() {};
  private static final BarrierIdentity ID2 = new BarrierIdentity() {};

  private int m_awakenCount = 0;

  private LocalBarrierGroups m_groups;

  @Before public void setUp() {
    m_groups = new LocalBarrierGroups();
  }

  @Test public void testCreateAndRetrieve() throws Exception {
    assertNull(m_groups.getExistingGroup("A"));

    final BarrierGroup a = m_groups.getGroup("A");
    assertEquals("A", a.getName());
    assertSame(a, m_groups.getGroup("A"));
    assertNotSame(a, m_groups.getGroup("B"));

    assertSame(a, m_groups.getExistingGroup("A"));

    a.addBarrier();
    a.removeBarriers(1); // Invalidate a.

    assertNotSame(a, m_groups.getGroup("A"));
  }

  private BarrierGroup createBarrierGroup(String groupName) {
    final BarrierGroup bg = m_groups.getGroup(groupName);

    bg.addListener(new Listener() {
        public void awaken() {
          ++m_awakenCount;
        }
      });

    return bg;
  }

  @Test public void testBarrierGroup() {
    final BarrierGroup bg = createBarrierGroup("Foo");

    assertEquals("Foo", bg.getName());
    assertEquals(0, m_awakenCount);
  }

  @Test public void testBarrierGroupAddWaiter() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    bg.addBarrier();
    bg.addBarrier();

    bg.addWaiter(ID1);
    assertEquals(0, m_awakenCount);

    bg.addWaiter(ID2);
    assertEquals(1, m_awakenCount);

    bg.addWaiter(ID2);
    assertEquals(1, m_awakenCount);

    bg.addWaiter(ID1);
    assertEquals(2, m_awakenCount);
  }

  @Test public void testRemoveBarriers() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    bg.addBarrier();
    bg.addBarrier();
    bg.addBarrier();

    bg.addWaiter(ID1);
    bg.addWaiter(ID2);
    assertEquals(0, m_awakenCount);

    bg.removeBarriers(1);
    assertEquals(1, m_awakenCount);
  }

  @Test public void testRemoveTooManyBarriers() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    bg.addBarrier();
    bg.addBarrier();
    bg.addBarrier();

    bg.removeBarriers(1);
    bg.removeBarriers(1);

    try {
      bg.removeBarriers(2);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }

    assertEquals(0, m_awakenCount);
  }

  @Test public void testInvalidGroup() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    bg.addBarrier();
    bg.removeBarriers(1);

    try {
      bg.addWaiter(null);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }

    try {
      bg.addBarrier();
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }

    try {
      bg.removeBarriers(1);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }

    assertEquals(0, m_awakenCount);
  }

  @Test public void addMoreWaitersThanBarriers() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    try {
      bg.addWaiter(ID2);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }
  }

  @Test public void testCancelWaiter() throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    bg.addBarrier();
    bg.addBarrier();

    bg.addWaiter(ID1);
    assertEquals(0, m_awakenCount);

    bg.cancelWaiter(ID2); // noop

    bg.cancelWaiter(ID1);

    bg.addWaiter(ID2);
    assertEquals(0, m_awakenCount);

    bg.addWaiter(ID1);
    assertEquals(1, m_awakenCount);
  }

  @Test public void testRemoveListener()  throws Exception {
    final BarrierGroup bg = createBarrierGroup("Foo");

    final AtomicInteger awakenCount = new AtomicInteger();

    final Listener listener = new Listener() {
      public void awaken() { awakenCount.incrementAndGet(); }
    };

    bg.addListener(listener);

    bg.addBarrier();
    bg.addWaiter(ID1);
    assertEquals(1, m_awakenCount);
    assertEquals(1, awakenCount.get());

    bg.removeListener(listener);

    bg.addWaiter(ID1);
    assertEquals(2, m_awakenCount);
    assertEquals(1, awakenCount.get());
  }
}
