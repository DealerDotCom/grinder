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

package net.grinder.plugin.http;

import net.grinder.common.GrinderException;
import net.grinder.common.Test;
import net.grinder.common.TestImplementation;
import net.grinder.plugininterface.RegisteredTest;
import net.grinder.script.AbortRunException;
import net.grinder.script.InvokeableTest;
import net.grinder.script.ScriptException;
import net.grinder.script.TestResult;


/**
 * 
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
public class HTTPTest extends TestImplementation implements InvokeableTest
{
    static HttpPlugin s_temporaryHack;

    private transient /* <-- FIX MY PARENT */ RegisteredTest m_registeredTest;

    public HTTPTest(int number, String description, String url)
	throws ScriptException
    {
	super(number, description);

	getParameters().setProperty("url", url);

	try {
	    m_registeredTest = s_temporaryHack.registerTest(this);
	}
	catch (GrinderException e) {
	    throw new ScriptException("Failed to register test", e);
	}
    }

    public TestResult invoke() throws AbortRunException
    {
	try {
	    return s_temporaryHack.invokeTest(m_registeredTest);
	}
	catch (GrinderException e) {
	    throw new AbortRunException(e.getMessage(), e);
	}
    }
}
