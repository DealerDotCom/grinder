// The Grinder
// Copyright (C) 2001  Paco Gomez
// Copyright (C) 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

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
	final GrinderProperties properties = new GrinderProperties();

	final TestImplementation testImplementation =
	    new TestImplementation(1, "description", properties);

	assertEquals(1, testImplementation.getNumber());
	assertEquals("description", testImplementation.getDescription());
	assertSame(properties, testImplementation.getParameters());
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
	    sorted.add(new TestImplementation(i, Integer.toString(i), null));
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
	final TestImplementation t1 =
	    new TestImplementation(57, "one thing", new GrinderProperties());

	final TestImplementation t2 =
	    new TestImplementation(57, "leads to", new GrinderProperties());

	final TestImplementation t3 =
	    new TestImplementation(58, "another", new GrinderProperties());

	assertEquals(t1, t2);
	assertEquals(t2, t1);
	assert(!t1.equals(t3));
	assert(!t3.equals(t1));
	assert(!t2.equals(t3));
	assert(!t3.equals(t2));
    }
}

