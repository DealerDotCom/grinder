// Copyright (C) 2003 Philip Aston
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

import java.util.Random;

import junit.framework.TestCase;
import junit.swingui.TestRunner;

import net.grinder.common.GrinderException;


/**
 * Unit test case for <code>EndPoint</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestEndPoint extends TestCase {

  public TestEndPoint(String name) {
    super(name);
  }

  public void testAccessors() throws Exception {

    final Random random = new Random();

    for (int i=0; i<10; ++i) {
      final byte[] bytes = new byte[random.nextInt(30)];
      random.nextBytes(bytes);
      final String hostname = new String(bytes);
      final int port = random.nextInt(65536);

      final EndPoint endPoint = new EndPoint(hostname, port);
      assertEquals(hostname.toLowerCase(), endPoint.getHost());
      assertEquals(port, endPoint.getPort());
    }
  }

  public void testEquality() throws Exception   {
    final EndPoint[] endPoint = {
      new EndPoint("Abcdef", 55),
      new EndPoint("aBCDef", 55),
      new EndPoint("c", 55),
      new EndPoint("a", 5512),
      new EndPoint("a", 56),
    };

    assertEquals(endPoint[0], endPoint[0]);
    assertEquals(endPoint[0], endPoint[1]);
    assertEquals(endPoint[1], endPoint[0]);
    assertTrue(!endPoint[0].equals(endPoint[2]));
    assertTrue(!endPoint[1].equals(endPoint[3]));
    assertTrue(!endPoint[1].equals(endPoint[4]));
  }
}
