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
public abstract class ExpressionView implements Comparable
{
    private final String m_displayName;
    private final String m_expressionString;

    /**
     * @clientCardinality 1
     * @link aggregationByValue
     * @supplierCardinality 1 
     **/
    private final StatisticExpression m_expression;

    /**
     *@link dependency 
     * @stereotype use
     **/
    /*#StatisticExpressionFactory lnkStatisticExpressionFactory;*/

    public ExpressionView(String displayName, String expressionString,
			  ProcessStatisticsIndexMap indexMap)
	throws GrinderException
    {
	m_displayName = displayName;
	m_expressionString = expressionString; // SHOULD NORMALISE.

	m_expression =
	    StatisticExpressionFactory.getInstance()
	    .createExpression(expressionString, indexMap);
    }

    public ExpressionView(ObjectInput in,
			  ProcessStatisticsIndexMap indexMap)
	throws GrinderException, IOException
    {
	this(in.readUTF(), in.readUTF(), indexMap);
    }

    public final void myWriteExternal(ObjectOutput out) throws IOException
    {
	out.writeUTF(m_displayName);
	out.writeUTF(m_expressionString);
    }

    public String getDisplayName()
    {
        return m_displayName;
    }

    public boolean equals(Object other)
    {
	if (other == this) {
	    return true;
	}

	if (!(other instanceof ExpressionView)) {
	    return false;
	}

	final ExpressionView otherView = (ExpressionView)other;

	return m_expressionString.equals(otherView.m_expressionString);
    }

    public final int compareTo(Object other)
    {
	// !TODO
	return 0;
    }
}
