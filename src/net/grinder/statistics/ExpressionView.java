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
 * Associate a {@link StatisticExpression} with display information.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class ExpressionView
{
    private static int s_creationOrder;
    private static StatisticExpressionFactory s_statisticExpressionFactory =
	StatisticExpressionFactory.getInstance();

    private final String m_displayName;
    private final String m_displayNameResourceKey;
    private final String m_expressionString;
    private final int m_hashCode;
    private final int m_creationOrder;

    /**
     * @clientCardinality 1
     * @link aggregationByValue
     * @supplierCardinality 1 
     **/
    private final StatisticExpression m_expression;

    /**
     * Creates a new <code>ExpressionView</code> instance.
     *
     * @param displayName A common display name.
     * @param displayNameResourceKey A resource key to use to look up
     * an internationalised display name.
     * @param expressionString An expression string, used to create
     * the {@link StatisticExpression}.
     * @exception GrinderException if an error occurs
     * @see StatisticExpressionFactory
     */
    public ExpressionView(String displayName, String displayNameResourceKey,
			  String expressionString)
	throws GrinderException
    {
	this(displayName, displayNameResourceKey,
	     s_statisticExpressionFactory.normaliseExpressionString(
		 expressionString),
	     s_statisticExpressionFactory.createExpression(expressionString));
    }

    /**
     * Creates a new <code>ExpressionView</code> instance.
     *
     * @param displayName A common display name.
     * @param displayNameResourceKey A resource key to use to look up
     * an internationalised display name.
     * @param expression A {@link StatisticExpression}.
     * @exception GrinderException if an error occurs
     */
    public ExpressionView(String displayName, String displayNameResourceKey,
			  StatisticExpression expression)
	throws GrinderException
    {
	this(displayName, displayNameResourceKey, "", expression);
    }

    private ExpressionView(String displayName, String displayNameResourceKey,
			   String expressionString,
			   StatisticExpression expression)
	throws GrinderException
    {
	m_displayName = displayName;
	m_displayNameResourceKey = displayNameResourceKey;
	m_expressionString = expressionString;
	m_expression = expression;

	m_hashCode =
	    m_displayName.hashCode() ^
	    m_displayNameResourceKey.hashCode() ^
	    m_expressionString.hashCode();	

	synchronized(ExpressionView.class) {
	    m_creationOrder = s_creationOrder++;
	}
    }

    /**
     * @see StatisticsView#readExternal
     **/
    ExpressionView(ObjectInput in)
	throws GrinderException, IOException
    {
	this(in.readUTF(), in.readUTF(), in.readUTF());
    }

    /**
     * @see StatisticsView#writeExternal
     **/
    final void myWriteExternal(ObjectOutput out) throws IOException
    {
	if (m_expressionString == "") {
	    throw new IOException(
		"This expression view is not externalisable");
	}

	out.writeUTF(m_displayName);
	out.writeUTF(m_displayNameResourceKey);
	out.writeUTF(m_expressionString);
    }

    /**
     * Get the common display name.
     *
     * @return The display name.
     **/
    public final String getDisplayName()
    {
        return m_displayName;
    }

    /**
     * Get the display name resource key.
     *
     * @return A key that might be used to look up an
     * internationalised display name.
     **/
    public final String getDisplayNameResourceKey()
    {
        return m_displayNameResourceKey;
    }

    /**
     * Return the {@link StatisticExpression}.
     *
     * @return The {@link StatisticExpression}.
     **/
    public final StatisticExpression getExpression()
    {
	return m_expression;
    }

    /**
     * Value based equality.
     *
     * @param other An <code>Object</code> to compare.
     * @return <code>true</code> => <code>other</code> is equal to this object.
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
	    m_hashCode == otherView.m_hashCode &&
	    m_displayName.equals(otherView.m_displayName) &&
	    m_displayNameResourceKey.equals(
		otherView.m_displayNameResourceKey) &&

	    // If either expression string is null, one of the views
	    // is not externalisable. We then only compare on the
	    // display names.
	    (m_expressionString.length() == 0 ||
	     otherView.m_expressionString.length() == 0 ||
	     m_expressionString.equals(otherView.m_expressionString));
    }

    /**
     * Implement {@link Object#hashCode}.
     *
     * @return an <code>int</code> value
     */
    public final int hashCode()
    {
	return m_hashCode;
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

    final int getCreationOrder()
    {
	return m_creationOrder;
    }
}
