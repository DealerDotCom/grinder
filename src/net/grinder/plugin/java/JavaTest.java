// Copyright (C) 2002 Philip Aston
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

package net.grinder.plugin.java;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.python.core.PyJavaInstance;
import org.python.core.PyObject;

import net.grinder.common.GrinderException;
import net.grinder.common.Test;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginTest;


/**
 * Represents an individual Java test. Scripts don't access this
 * directly, instead they get a proxy from <code>JavaPlugin</code>;
 * see {@link JavaPlugin#createTest}.
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
class JavaTest extends PluginTest
{
    private final transient Object m_target;
    private final PyObject m_pySelf;

    public JavaTest(int number, String description, Object target)
	throws GrinderException
    {
	super(JavaPlugin.class, number, description);

	m_target = target;
	m_pySelf = new PyJavaInstance(this);
    }

    Object getProxy() 
    {
	return new TestPyJavaInstance(m_pySelf, m_target) {
		public PyObject dispatch(
		    TestPyJavaInstance.Closure parameters) {
		    try {
			return (PyObject)JavaTest.this.invokeTest(parameters);
		    }
		    catch (GrinderException e) {
			// FIX ME
			throw new RuntimeException("Dispatch failed " + e);
		    }
		}
	    };
    }
}
