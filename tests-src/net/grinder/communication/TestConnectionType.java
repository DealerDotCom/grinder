// Copyright (C) 2003, 2004 Philip Aston
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

package net.grinder.communication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import junit.framework.TestCase;


/**
 *  Unit test case for <code>ConnectionType</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestConnectionType extends TestCase {

  public TestConnectionType(String name) {
    super(name);
  }

  public void testEquality() throws Exception {
    assertEquals(ConnectionType.CONTROL.hashCode(),
                 ConnectionType.CONTROL.hashCode());

    assertEquals(ConnectionType.CONTROL, ConnectionType.CONTROL);

    assertEquals(ConnectionType.REPORT.hashCode(),
                 ConnectionType.REPORT.hashCode());

    assertEquals(ConnectionType.REPORT, ConnectionType.REPORT);

    assertTrue(!ConnectionType.CONTROL.equals(ConnectionType.REPORT));
    assertTrue(!ConnectionType.REPORT.equals(ConnectionType.CONTROL));

    assertTrue(!ConnectionType.REPORT.equals(new Object()));
  }

  public void testSerialisation() throws Exception {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ConnectionType.CONTROL.write(outputStream);
    ConnectionType.REPORT.write(outputStream);

    final InputStream inputSteam =
      new ByteArrayInputStream(outputStream.toByteArray());

    assertEquals(ConnectionType.CONTROL, ConnectionType.read(inputSteam));
    assertEquals(ConnectionType.REPORT, ConnectionType.read(inputSteam));
  }
}
