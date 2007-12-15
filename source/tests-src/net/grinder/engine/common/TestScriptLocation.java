// Copyright (C) 2007 Philip Aston
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

package net.grinder.engine.common;

import java.io.File;

import net.grinder.testutility.Serializer;

import junit.framework.TestCase;


/**
 * Unit tests for {@link  ScriptLocation}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestScriptLocation extends TestCase {

  public void testScriptLocation() throws Exception {

    final File f1 = new File("abc");
    final File f2 = new File("def");
    final File f3 = new File("blah");

    final ScriptLocation sl1 = new ScriptLocation(f1, f2);
    final ScriptLocation sl2 = new ScriptLocation(f1, f2);
    final ScriptLocation sl3 = new ScriptLocation(f1, f3);

    assertEquals(sl1, sl1);
    assertEquals(sl1, sl2);
    assertTrue(!sl1.equals(sl3));
    assertTrue(!sl1.equals(this));
    assertEquals(sl1.hashCode(), sl2.hashCode());
    assertTrue(sl1.hashCode() != sl3.hashCode());

    final ScriptLocation sl4 = (ScriptLocation)Serializer.serialize(sl1);
    assertEquals(sl1, sl4);
  }
}
