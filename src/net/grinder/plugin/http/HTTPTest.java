// Copyright (C) 2001, 2002 Philip Aston
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
