// Copyright (C) 2002 Philip Aston
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
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import HTTPClient.NVPair;

import net.grinder.engine.process.StubProcessContext;


/**
 * Unit test case for <code>HTTPTest</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class TestHTTPTest extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestHTTPTest.class);
    }

    public TestHTTPTest(String name)
    {
	super(name);
    }

    protected void setUp() throws Exception 
    {
	StubProcessContext.get();
    }

    public void testIsSerializable() throws Exception
    {
	final NVPair[] formData = {
	    new NVPair("foo", "bar"),
	};

	final HTTPTest httpTest = new HTTPTest(123, "test");
	httpTest.setUrl("http://grinder.sf.net");
	httpTest.setFormData(formData);

	final ByteArrayOutputStream byteArrayOutputStream =
	    new ByteArrayOutputStream();

	final ObjectOutputStream objectOutputStream =
	    new ObjectOutputStream(byteArrayOutputStream);

	objectOutputStream.writeObject(httpTest);
    }
}
