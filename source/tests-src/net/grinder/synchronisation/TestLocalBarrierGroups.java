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

import static net.grinder.testutility.AssertUtilities.assertNotEquals;
import static net.grinder.testutility.Serializer.serialize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import net.grinder.synchronisation.BarrierGroup.BarrierIdentity;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentityGenerator;
import net.grinder.synchronisation.BarrierGroup.Listener;

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

  private BarrierGroups m_groups;

  @Before public void setUp() {
    m_groups = new LocalBarrierGroups();
  }

  @Test public void testCreateAndRetrieve() {
    final BarrierGroup a = m_groups.getGroup("A");
    assertEquals("A", a.getName());
    assertSame(a, m_groups.getGroup("A"));
    assertNotSame(a, m_groups.getGroup("B"));

    a.addBarrier();
    a.removeBarriers(1); // Invalidate a.

    assertNotSame(a, m_groups.getGroup("A"));
  }

  @Test public void testIdentityGeneration() {
    final BarrierIdentityGenerator generator = m_groups.getIdentityGenerator();

    final BarrierIdentity one = generator.next();
    final BarrierIdentity two = generator.next();

    assertNotEquals(one, two);
  }

  @Test public void testIdentityIsSerializable() throws Exception {
    final BarrierIdentityGenerator generator = m_groups.getIdentityGenerator();

    final BarrierIdentity id = generator.next();

    final BarrierIdentity serializedID = serialize(id);

    assertEquals(id, serializedID);
  }

  @Test public void testIdentityEquality() throws Exception {
    final BarrierIdentityGenerator generator = m_groups.getIdentityGenerator();

    final BarrierIdentity one = generator.next();
    final BarrierIdentity two = generator.next();

    assertEquals(one, one);
    assertNotEquals(one, two);
    assertNotEquals(one, this);
    assertNotEquals(one, null);
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

  @Test public void testBarrierGroupAddWaiter() {
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

  @Test public void testRemoveBarriers() {
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

  @Test public void testRemoveTooManyBarriers() {
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

  @Test public void testInvalidGroup() {
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

  @Test public void addMoreWaitersThanBarriers() {
    final BarrierGroup bg = createBarrierGroup("Foo");

    try {
      bg.addWaiter(ID2);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }
  }

  @Test public void testCancelWaiter() {
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

  @Test public void testRemoveListener() {
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
