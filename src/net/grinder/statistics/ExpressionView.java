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
import java.io.Serializable;

import net.grinder.common.GrinderException;


/**
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class ExpressionView implements Comparable
{
    private static int s_creationOrder;
    private static StatisticExpressionFactory s_statisticExpressionFactory =
	StatisticExpressionFactory.getInstance();

    private final String m_displayName;
    private final String m_displayNameResourceKey;
    private final String m_expressionString;
    private final int m_creationOrder;

    /**
     * @clientCardinality 1
     * @link aggregationByValue
     * @supplierCardinality 1 
     **/
    private final StatisticExpression m_expression;

    public ExpressionView(String displayName, String displayNameResourceKey,
			  String expressionString,
			  StatisticsIndexMap indexMap)
	throws GrinderException
    {
	m_displayName = displayName;
	m_displayNameResourceKey = displayNameResourceKey;

	m_expressionString =
	    s_statisticExpressionFactory.normaliseExpressionString(
		expressionString);

	m_expression =
	    s_statisticExpressionFactory.createExpression(expressionString,
							  indexMap);

	synchronized(ExpressionView.class) {
	    m_creationOrder = s_creationOrder++;
	}
    }

    public ExpressionView(ObjectInput in,
			  StatisticsIndexMap indexMap)
	throws GrinderException, IOException
    {
	this(in.readUTF(), in.readUTF(), in.readUTF(), indexMap);
    }

    public final void myWriteExternal(ObjectOutput out) throws IOException
    {
	out.writeUTF(m_displayName);
	out.writeUTF(m_displayNameResourceKey);
	out.writeUTF(m_expressionString);
    }

    public final String getDisplayName()
    {
        return m_displayName;
    }

    public final String getDisplayNameResourceKey()
    {
        return m_displayNameResourceKey;
    }

    public final StatisticExpression getExpression()
    {
	return m_expression;
    }

    /**
     * Value based equality.
     **/
    public boolean equals(Object other)
    {
	if (other == this) {
	    return true;
	}

	if (!(other instanceof ExpressionView)) {
	    return false;
	}

	final ExpressionView otherView = (ExpressionView)other;

	return
	    m_expressionString.equals(otherView.m_expressionString) &&
	    m_displayName.equals(otherView.m_displayName) &&
	    m_displayNameResourceKey.equals(
		otherView.m_displayNameResourceKey);
    }

    /**
    * The JDK documentation says I have to say "<em>Note: this class
    * has a natural ordering that is inconsistent with equals</em>. In
    * our case <code>x.compareTo(y)==0) => (x.equals(y))</code>, but
    * the reverse implication does not hold.
    **/
    public final int compareTo(Object otherObject)
    {
	final ExpressionView other = (ExpressionView)otherObject;

	if (m_expression.isPrimitive() &&
	    !other.m_expression.isPrimitive()) {
	    return -1;
	}
	else if (!m_expression.isPrimitive() &&
		 other.m_expression.isPrimitive()) {
	    return 1;
	}

	if (m_creationOrder < other.m_creationOrder) {
	    return -1;
	}
	else if (m_creationOrder > other.m_creationOrder) {
	    return 1;
	}
	else {
	    // Should assert ? Same creation order => same instance.
	    return 0;
	}
    }

    /**
     * Return a <code>String</code> representation of this
     * <code>ExpressionView</code>.
     *
     * @return The <code>String</code>
     **/
    public final String toString()
    {
	return
	    "ExpressionView(" + m_displayName + ", " +
	    m_displayNameResourceKey + ", " + m_expressionString + ")";
    }
}
