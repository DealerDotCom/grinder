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

package net.grinder.tools.tcpproxy;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import junit.framework.TestCase;


/**
 * Unit test case for {@link NullFilter}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestNullFilter extends TestCase {

  private final ConnectionDetails m_connectionDetails =
      new ConnectionDetails(new EndPoint("host1", 1234),
                            new EndPoint("host2", 99),
                            false);

  private ByteArrayOutputStream m_output = new ByteArrayOutputStream();

  public void testHandle() throws Exception {
    final NullFilter nullFilter =
      new NullFilter(new PrintWriter(m_output,  true));

    final String input = new String();

    nullFilter.handle(m_connectionDetails, "This is a campaign.".getBytes(), 5);
    assertNoOutput();

    final String lines = "Some\nlines\rblah";
    nullFilter.handle(m_connectionDetails, lines.getBytes(), lines.length());
    assertNoOutput();

    final byte[] binary = { 0x01, (byte)0xE7, 'a', 'b', 'c', (byte)0x89,
                            'd', 'a', 'h', '\n', 'b', 'a', 'h' };
    nullFilter.handle(m_connectionDetails, binary, binary.length);
    assertNoOutput();
  }

  public void testOtherMethods() throws Exception {
    final NullFilter nullFilter =
      new NullFilter(new PrintWriter(m_output,  true));

    nullFilter.connectionOpened(m_connectionDetails);
    nullFilter.connectionClosed(m_connectionDetails);
    nullFilter.stop();

    assertNoOutput();
  }

  private void assertNoOutput() {
    assertEquals("'" + m_output.toString() + "' is empty.", 0, m_output.size());
  }
}