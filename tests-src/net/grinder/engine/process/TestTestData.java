// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
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

package net.grinder.engine.process;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import net.grinder.common.GrinderProperties;
import net.grinder.common.Test;
import net.grinder.common.TestImplementation;


/**
 * Unit test case for <code>TestTestData</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public class TestTestData extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestTestData.class);
    }

    public TestTestData(String name)
    {
	super(name);
    }

    protected void setUp()
    {
    }

    public void test0() throws Exception
    {
	final Test test = new TestImplementation(99, "Some stuff");
	
	final TestData testData = new TestData(test);
	assertEquals(test, testData.getTest());
	assertNotNull(testData.getStatistics());
    }

    public void test1() throws Exception
    {
	final Test test = new TestImplementation(-33, "");
	test.getParameters().put("Something", "blah");

	final TestData testData = new TestData(test);
	assertEquals(test, testData.getTest());
	assertNotNull(testData.getStatistics());
    }
}
