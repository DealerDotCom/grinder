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

package net.grinder.common;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestTestImplementation extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestTestImplementation.class);
    }

    public TestTestImplementation(String name)
    {
	super(name);
    }

    protected void setUp()
    {
    }

    public void testGetters()
    {
	final TestImplementation testImplementation =
	    new TestImplementation(1, "description");

	assertEquals(1, testImplementation.getNumber());
	assertEquals("description", testImplementation.getDescription());
    }

    public void testOrdering()
    {
	final int size = 100;

	final Set sorted = new TreeSet();
	final List keys = new ArrayList(size);

	for (int i=0; i<size; i++) {
	    keys.add(new Integer(i));
	}
	
	Collections.shuffle(keys);

	final Iterator keyIterator = keys.iterator();

	while (keyIterator.hasNext()) {
	    final int i = ((Integer)keyIterator.next()).intValue();
	    sorted.add(new TestImplementation(i, Integer.toString(i)));
	}
	
	final Iterator sortedIterator = sorted.iterator();
	int i = 0;

	while (keyIterator.hasNext()) {
	    final TestImplementation testImplementation =
		(TestImplementation)sortedIterator.next();

	    assertEquals(i++, testImplementation.getNumber());
	}
    }

    public void testEquality()
    {
	// Equality depends only on test number.
	final TestImplementation t1 = new TestImplementation(57, "one thing");
	final TestImplementation t2 = new TestImplementation(57, "leads to");
	final TestImplementation t3 = new TestImplementation(58, "another");

	assertEquals(t1, t2);
	assertEquals(t2, t1);
	assertTrue(!t1.equals(t3));
	assertTrue(!t3.equals(t1));
	assertTrue(!t2.equals(t3));
	assertTrue(!t3.equals(t2));
    }
}

