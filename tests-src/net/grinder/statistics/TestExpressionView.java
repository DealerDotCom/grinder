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

package net.grinder.statistics;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.SortedSet;
import java.util.TreeSet;

import net.grinder.common.GrinderException;


/**
 * Unit test case for <code>StatisticExpressionFactory</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see RawStatistics
 */
public class TestExpressionView extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestExpressionView.class);
    }

    public TestExpressionView(String name)
    {
	super(name);
    }

    protected void setUp() throws Exception
    {
    }

    public void testConstruction() throws Exception
    {
	final ExpressionView view =
	    new ExpressionView("My view", "my.view",
			       "(+ userLong0 userLong1)");

	assertEquals("My view", view.getDisplayName());
	assertEquals("my.view", view.getDisplayNameResourceKey());
	assertTrue(view.getExpression() != null);

	final StatisticExpressionFactory statisticExpressionFactory =
	    StatisticExpressionFactory.getInstance();
	final ExpressionView view2 =
	    new ExpressionView("My view2", "my.view", 
			       statisticExpressionFactory.createExpression(
				   "userLong0"));

	assertEquals("My view2", view2.getDisplayName());
	assertEquals("my.view", view2.getDisplayNameResourceKey());
	assertTrue(view.getExpression() != null);
    }

    public void testEquality() throws Exception
    {
	final ExpressionView[] views = {
	    new ExpressionView("My view", "my.view", 
			       "(+ userLong0 userLong1)"),
	    new ExpressionView("My view", "my.view",
			       "(+ userLong0 userLong1)"),
	    new ExpressionView("My view", "my.view",
			       "(+ userLong0 userLong2)"),
	    new ExpressionView("My View", "my.view",
			       "(+ userLong0 userLong1)"),
	    new ExpressionView("My view", "my view",
			       "(+ userLong0 userLong1)"),
	};

	assertEquals(views[0], views[1]);
	assertEquals(views[1], views[0]);
	assertTrue(!views[0].equals(views[2]));
	assertTrue(!views[1].equals(views[3]));
	assertTrue(!views[1].equals(views[4]));
    }

    public void testExternalisation() throws Exception
    {
	final ExpressionView original =
	    new ExpressionView("My view", "my.view", "(+ userLong0 userLong1)");

	final ByteArrayOutputStream byteOutputStream =
	    new ByteArrayOutputStream();

	final ObjectOutputStream objectOutputStream =
	    new ObjectOutputStream(byteOutputStream);

	original.myWriteExternal(objectOutputStream);
	objectOutputStream.close();

	final ObjectInputStream objectInputStream =
	    new ObjectInputStream(
		new ByteArrayInputStream(byteOutputStream.toByteArray()));

	final ExpressionView received = new ExpressionView(objectInputStream);

	assertEquals(original, received);

	final StatisticExpressionFactory statisticExpressionFactory =
	    StatisticExpressionFactory.getInstance();
	final ExpressionView cantStreamThis =
	    new ExpressionView("My view2", "my.view", 
			       statisticExpressionFactory.createExpression(
				   "userLong0"));

	try {
	    cantStreamThis.myWriteExternal(objectOutputStream);
	    fail("Expected an IOException");
	}
	catch (IOException e) {
	}
    }
}
