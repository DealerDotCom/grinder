// Copyright (C) 2004 Philip Aston
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

package net.grinder.console.communication;

import junit.framework.TestCase;

import java.net.InetAddress;

import net.grinder.communication.ConnectionIdentity;
import net.grinder.common.ProcessStatus;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;


/**
 *  Unit test case for {@link DistributionStatus}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestDistributionStatus extends TestCase {

  private ConnectionIdentity m_connection1;
  private ConnectionIdentity m_connection2;

  protected void setUp() throws Exception {
    final InetAddress host = InetAddress.getLocalHost();

    m_connection1 = new ConnectionIdentity(host, 123, 432);
    m_connection2 = new ConnectionIdentity(host, 123, 999);
  }

  public void testGetEarliestLastModifiedTime() throws Exception {
    final DistributionStatus distributionStatus = new DistributionStatus();

    assertEquals(Long.MAX_VALUE,
                 distributionStatus.getEarliestLastModifiedTime());

    distributionStatus.set(m_connection1, 33);
    distributionStatus.set(m_connection2, 31221);
    distributionStatus.set(m_connection1, 44);

    assertEquals(44L, distributionStatus.getEarliestLastModifiedTime());

    distributionStatus.remove(m_connection1);

    assertEquals(31221L, distributionStatus.getEarliestLastModifiedTime());
  }

  public void testSetAll() throws Exception {
    final DistributionStatus distributionStatus = new DistributionStatus();

    distributionStatus.set(m_connection1, 33);
    distributionStatus.set(m_connection2, 31221);

    assertEquals(33L, distributionStatus.getEarliestLastModifiedTime());

    distributionStatus.setAll(100);

    assertEquals(100L, distributionStatus.getEarliestLastModifiedTime());
  }
}
