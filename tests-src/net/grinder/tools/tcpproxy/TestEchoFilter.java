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
 * Unit test case for {@link EchoFilter}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestEchoFilter extends TestCase {

  private static final String LINE_SEPARATOR =
      System.getProperty("line.separator");

  private final EndPoint m_endPoint1 = new EndPoint("host1", 1234);
  private final EndPoint m_endPoint2 = new EndPoint("host2", 99);

  private final ConnectionDetails m_connectionDetails =
      new ConnectionDetails(m_endPoint1, m_endPoint2, false);


  private ByteArrayOutputStream m_output = new ByteArrayOutputStream();

  public void testHandle() throws Exception {
    final EchoFilter echoFilter =
      new EchoFilter(new PrintWriter(m_output,  true));

    final String input = new String();

    echoFilter.handle(m_connectionDetails, "This is a campaign.".getBytes(), 5);

    assertOutput(m_connectionDetails.toString());
    assertOutput("This " + LINE_SEPARATOR);
    assertNoOutput();

    final String lines = "Some\nlines\rblah";
    echoFilter.handle(m_connectionDetails, lines.getBytes(), lines.length());

    assertOutput(m_connectionDetails.toString());
    assertOutput("Some\nlines\rblah" + LINE_SEPARATOR);
    assertNoOutput();

    final byte[] binary = { 0x01, (byte)0xE7, 'a', 'b', 'c', (byte)0x89,
                            'd', 'a', 'h', '\n', 'b', 'a', 'h' };
    echoFilter.handle(m_connectionDetails, binary, binary.length);
    assertOutput(m_connectionDetails.toString());
    assertOutput("[01E7]abc[89]dah\nbah" + LINE_SEPARATOR);
    assertNoOutput();
  }

  public void testOtherMethods() throws Exception {
    final EchoFilter echoFilter =
      new EchoFilter(new PrintWriter(m_output,  true));

    assertNoOutput();

    echoFilter.connectionOpened(m_connectionDetails);
    assertOutput(m_connectionDetails.toString());
    assertOutput("opened");
    assertEquals(1, getNumberOfOutputLines());

    m_output.reset();
    echoFilter.connectionClosed(m_connectionDetails);
    assertOutput(m_connectionDetails.toString());
    assertOutput("closed");
    assertEquals(1, getNumberOfOutputLines());

    m_output.reset();
    echoFilter.stop();
    assertNoOutput();
  }

  private void assertNoOutput() {
    assertEquals("'" + m_output.toString() + "' is empty.", 0, m_output.size());
  }

  private void assertOutput(String substring) throws Exception {
    final String outputAsString = m_output.toString();
    final int index = outputAsString.indexOf(substring);

    assertTrue("'" + outputAsString + "' contains '" + substring + "'",
               index >= 0);

    final int remainderStart = index + substring.length();
    final byte[] bytes = m_output.toByteArray();
    final byte[] remainder = new byte[bytes.length - remainderStart];
    System.arraycopy(bytes, remainderStart, remainder, 0, remainder.length);
    m_output.reset();
    m_output.write(remainder);
  }

  private int getNumberOfOutputLines() {
    final String outputAsString = m_output.toString();

    int result = 0;
    int index = -1;

    while (true) {
      index = outputAsString.indexOf(LINE_SEPARATOR, index + 1);

      if (index < 0) {
        break;
      }

      ++result;
    }

    return result;
  }
}
