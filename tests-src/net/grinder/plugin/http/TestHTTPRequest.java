// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

package net.grinder.plugin.http;

import junit.framework.TestCase;

import HTTPClient.NVPair;


/**
 * Unit test case for <code>HTTPRequest</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestHTTPRequest extends TestCase {

  public TestHTTPRequest(String name) {
    super(name);
  }

  public void testSetHeaders() {
    final HTTPRequest httpRequest = new HTTPRequest();

    assertEquals(0, httpRequest.getHeaders().length);

    final NVPair[] newHeaders = new NVPair[] {
      new NVPair("name", "value"),
      new NVPair("another name", "another value"),
    };
    
    httpRequest.setHeaders(newHeaders);
    assertArraysEqual(newHeaders, httpRequest.getHeaders());
  }

  public void testAddHeader() {

    final HTTPRequest httpRequest = new HTTPRequest();

    final NVPair[] newHeaders = new NVPair[] {
      new NVPair("name", "value"),
      new NVPair("another name", "another value"),
    };

    httpRequest.setHeaders(newHeaders);
    
    httpRequest.addHeader("name", "value");
    httpRequest.addHeader("foo", "bah");

    assertArraysEqual(new NVPair[] {
                        new NVPair("name", "value"),
                        new NVPair("another name", "another value"),
                        new NVPair("name", "value"),
                        new NVPair("foo", "bah"),
                      },
                      httpRequest.getHeaders());
  }

  public void testDeleteHeader() {

    final HTTPRequest httpRequest = new HTTPRequest();

    final NVPair[] newHeaders = new NVPair[] {
      new NVPair("name", "value"),
      new NVPair("another name", "another value"),
      new NVPair("name", "value"),
      new NVPair("some more stuff", "value"),
    };

    httpRequest.setHeaders(newHeaders);
    
    httpRequest.deleteHeader("name");

    assertArraysEqual(new NVPair[] {
                        new NVPair("another name", "another value"),
                        new NVPair("some more stuff", "value"),
                      },
                      httpRequest.getHeaders());

    httpRequest.deleteHeader("some more stuff");

    assertArraysEqual(new NVPair[] {
                        new NVPair("another name", "another value"),
                      },
                      httpRequest.getHeaders());
  }

  private static void assertArraysEqual(NVPair[] a, NVPair[] b) {
    
    assertTrue("Arrays of equal length", a.length == b.length);

    for (int i=0; i<a.length; ++i) {
      assertEquals("NVPair " + i + " name matches", 
                   a[i].getName(), b[i].getName());
      assertEquals("NVPair " + i + " value matches",
                   a[i].getValue(), b[i].getValue());
    }
  }

}
