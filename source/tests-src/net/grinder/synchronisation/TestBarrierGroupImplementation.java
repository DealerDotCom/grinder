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
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import net.grinder.synchronisation.BarrierGroup.BarrierIdentity;
import net.grinder.synchronisation.BarrierGroup.Listener;

import org.junit.Before;
import org.junit.Test;


/**
 * Unit tests for {@link BarrierGroupImplementation}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestBarrierGroupImplementation {

  private static final BarrierIdentity ID1 = new BarrierIdentity() {};
  private static final BarrierIdentity ID2 = new BarrierIdentity() {};

  private BarrierGroup m_bg;
  private int m_awakenCount = 0;

  @Before public void setUp() {
    m_bg = new BarrierGroupImplementation("Foo");

    m_bg.addListener(new Listener() {
      public void awaken() {
        ++m_awakenCount;
      }
    });
  }

  @Test public void testConstructon() {
    assertEquals("Foo", m_bg.getName());
    assertEquals(0, m_awakenCount);
  }

  @Test public void testAddWaiter() {
    m_bg.addBarrier();
    m_bg.addBarrier();

    m_bg.addWaiter(ID1);
    assertEquals(0, m_awakenCount);

    m_bg.addWaiter(ID2);
    assertEquals(1, m_awakenCount);

    m_bg.addWaiter(ID2);
    assertEquals(1, m_awakenCount);

    m_bg.addWaiter(ID1);
    assertEquals(2, m_awakenCount);
  }

  @Test public void testRemoveBarriers() {
    m_bg.addBarrier();
    m_bg.addBarrier();
    m_bg.addBarrier();

    m_bg.addWaiter(ID1);
    m_bg.addWaiter(ID2);
    assertEquals(0, m_awakenCount);

    m_bg.removeBarriers(1);
    assertEquals(1, m_awakenCount);
  }

  @Test public void testRemoveTooManyBarriers() {
    m_bg.addBarrier();
    m_bg.addBarrier();
    m_bg.addBarrier();

    m_bg.removeBarriers(1);
    m_bg.removeBarriers(2);

    try {
      m_bg.removeBarriers(1);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }

    assertEquals(0, m_awakenCount);
  }

  @Test public void addMoreWaitersThanBarriers() {
    try {
      m_bg.addWaiter(ID2);
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }
  }

  @Test public void testCancelWaiter() {
    m_bg.addBarrier();
    m_bg.addBarrier();

    m_bg.addWaiter(ID1);
    assertEquals(0, m_awakenCount);

    m_bg.cancelWaiter(ID2); // noop

    m_bg.cancelWaiter(ID1);

    m_bg.addWaiter(ID2);
    assertEquals(0, m_awakenCount);

    m_bg.addWaiter(ID1);
    assertEquals(1, m_awakenCount);
  }

  @Test public void testRemoveListener() {

    final AtomicInteger awakenCount = new AtomicInteger();

    final Listener listener = new Listener() {
      public void awaken() { awakenCount.incrementAndGet(); }
    };

    m_bg.addListener(listener);

    m_bg.addBarrier();
    m_bg.addWaiter(ID1);
    assertEquals(1, m_awakenCount);
    assertEquals(1, awakenCount.get());

    m_bg.removeListener(listener);

    m_bg.addWaiter(ID1);
    assertEquals(2, m_awakenCount);
    assertEquals(1, awakenCount.get());
  }
}
