// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

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

package net.grinder.statistics;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import net.grinder.common.GrinderException;


/**
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class StatisticsView implements Externalizable {
    /**
     * @link aggregation
     * @associates <{net.grinder.statistics.ExpressionView}>
     * @supplierCardinality 0..*
     */
    private final SortedSet m_columns;

    public StatisticsView()
    {
	m_columns = new TreeSet();
    }

    public final synchronized void add(StatisticsView other)
    {
	m_columns.addAll(other.m_columns);
    }

    public final synchronized void add(ExpressionView statistic)
    {
	m_columns.add(statistic);
    }

    public final synchronized ExpressionView[] getExpressionViews()
    {
	return (ExpressionView[])m_columns.toArray(new ExpressionView[0]);
    }

    /**
     * Externalisation method.
     *
     * @param out Handle to the output stream.
     * @exception IOException If an I/O error occurs.
     **/
    public synchronized void writeExternal(ObjectOutput out) throws IOException
    {
	out.writeInt(m_columns.size());

	final Iterator iterator = m_columns.iterator();

	while (iterator.hasNext()) {
	    final ExpressionView view = (ExpressionView)iterator.next();
	    view.myWriteExternal(out);
	}
    }

    /**
     * Externalisation method.
     *
     * @param in Handle to the input stream.
     * @exception IOException If an I/O error occurs.
     **/
    public synchronized void readExternal(ObjectInput in) throws IOException
    {
	final int n = in.readInt();

	m_columns.clear();

	for (int i=0; i<n; i++) {
	    try {
		add(new ExpressionView(in));
	    }
	    catch (GrinderException e) {
		throw new IOException(
		    "Could not instantiate ExpressionView: " + e.getMessage());
	    }
	}
    }
}
