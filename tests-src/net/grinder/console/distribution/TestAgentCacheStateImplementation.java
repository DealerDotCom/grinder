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

package net.grinder.console.distribution;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test for {@link AgentCacheStateImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestAgentCacheStateImplementation extends AbstractFileTestCase {

  public void testAgentCacheStateImplementation() throws Exception {

    final FileDistribution fileDistribution = new FileDistributionImplementation(null);

    final AgentCacheStateImplementation cacheState =
      (AgentCacheStateImplementation)fileDistribution.getAgentCacheState();

    final RandomStubFactory propertyChangeListenerStubFactory =
      new RandomStubFactory(PropertyChangeListener.class);
    final PropertyChangeListener propertyChangeListener =
      (PropertyChangeListener)propertyChangeListenerStubFactory.getStub();

    cacheState.addListener(propertyChangeListener);

    assertEquals(-1, cacheState.getEarliestFileTime());
    assertTrue(cacheState.getOutOfDate());
    propertyChangeListenerStubFactory.assertNoMoreCalls();

    cacheState.updateStarted();
    assertEquals(-1, cacheState.getEarliestFileTime());
    assertTrue(cacheState.getOutOfDate());
    propertyChangeListenerStubFactory.assertNoMoreCalls();

    cacheState.updateComplete();
    assertFalse(cacheState.getOutOfDate());
    final long earliestFileTime = cacheState.getEarliestFileTime();
    assertFalse(earliestFileTime == -1);
    propertyChangeListenerStubFactory.assertSuccess("propertyChange",
                                                    PropertyChangeEvent.class);
    propertyChangeListenerStubFactory.assertNoMoreCalls();

    cacheState.setOutOfDate();
    assertTrue(cacheState.getOutOfDate());
    assertEquals(earliestFileTime, earliestFileTime);
    propertyChangeListenerStubFactory.assertSuccess("propertyChange",
                                                    PropertyChangeEvent.class);
    propertyChangeListenerStubFactory.assertNoMoreCalls();

    Thread.sleep(10);

    cacheState.updateStarted();
    assertTrue(cacheState.getOutOfDate());
    assertEquals(earliestFileTime, earliestFileTime);
    propertyChangeListenerStubFactory.assertNoMoreCalls();

    cacheState.setOutOfDate();
    assertTrue(cacheState.getOutOfDate());
    assertEquals(earliestFileTime, earliestFileTime);
    propertyChangeListenerStubFactory.assertNoMoreCalls();

    cacheState.updateComplete();
    assertTrue(cacheState.getOutOfDate());
    assertFalse(earliestFileTime == cacheState.getEarliestFileTime());
    propertyChangeListenerStubFactory.assertNoMoreCalls();
  }
}
