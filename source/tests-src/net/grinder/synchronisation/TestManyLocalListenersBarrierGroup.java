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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentity;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link ManyLocalListenersBarrierGroup}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestManyLocalListenersBarrierGroup {

  private static final BarrierIdentity ID1 = new BarrierIdentity() {};

  private @Mock BarrierGroup m_delegate;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test public void testAddBarrier() {
    final ManyLocalListenersBarrierGroup group =
      new ManyLocalListenersBarrierGroup(m_delegate);
    verify(m_delegate).addListener(group);

    group.addBarrier();

    verify(m_delegate).addBarrier();
    verifyNoMoreInteractions(m_delegate);
  }

  @Test public void testRemoveBarriers() {
    final ManyLocalListenersBarrierGroup group =
      new ManyLocalListenersBarrierGroup(m_delegate);
    verify(m_delegate).addListener(group);

    group.removeBarriers(2);

    verify(m_delegate).removeBarriers(2);
    verifyNoMoreInteractions(m_delegate);
  }

  @Test public void testAddWaiter() {
    final ManyLocalListenersBarrierGroup group =
      new ManyLocalListenersBarrierGroup(m_delegate);
    verify(m_delegate).addListener(group);

    group.addWaiter(ID1);

    verify(m_delegate).addWaiter(ID1);
    verifyNoMoreInteractions(m_delegate);
  }

  @Test public void testCancelWaiter() {
    final ManyLocalListenersBarrierGroup group =
      new ManyLocalListenersBarrierGroup(m_delegate);
    verify(m_delegate).addListener(group);

    group.cancelWaiter(ID1);

    verify(m_delegate).cancelWaiter(ID1);
    verifyNoMoreInteractions(m_delegate);
  }

  @Test public void testGetName() {
    when(m_delegate.getName()).thenReturn("foo");

    final String name =
      new ManyLocalListenersBarrierGroup(m_delegate).getName();

    assertEquals("foo", name);
  }

  @Test public void testListeners() {
    final ManyLocalListenersBarrierGroup group =
      new ManyLocalListenersBarrierGroup(m_delegate);
    verify(m_delegate).addListener(group);

    final BarrierGroup.Listener listener1 = mock(BarrierGroup.Listener.class);
    final BarrierGroup.Listener listener2 = mock(BarrierGroup.Listener.class);

    group.addListener(listener1);
    group.addListener(listener2);

    verifyNoMoreInteractions(listener1, listener2);

    group.awaken();

    verify(listener1).awaken();
    verify(listener2).awaken();

    verifyNoMoreInteractions(listener1, listener2, m_delegate);
  }
}
