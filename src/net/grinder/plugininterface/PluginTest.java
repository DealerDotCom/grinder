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

package net.grinder.plugininterface;

import java.io.Serializable;

import net.grinder.common.AbstractTestSemantics;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.engine.process.TestRegistry;
import net.grinder.script.InvokeableTest;


/**
 * <p>Abstract base class for plugin test implementations.</p>
 * 
 * @author Philip Aston
 * @version $Revision$
 **/
public abstract class PluginTest
    extends AbstractTestSemantics
    implements InvokeableTest, Serializable
{
    private final int m_number;
    private final String m_description;
    private final transient GrinderProperties m_parameters = 
	new GrinderProperties();

    private transient final TestRegistry.RegisteredTest m_registeredTest;

    public PluginTest(Class pluginClass, int number, String description)
	throws GrinderException
    {
	m_number = number;
	m_description = description;

	m_registeredTest =
	    TestRegistry.getInstance().register(pluginClass, this);
    }

    public final GrinderPlugin getPlugin()
    {
	return m_registeredTest.getPlugin();
    }

    public final int getNumber()
    {
	return m_number;
    }

    public final String getDescription()
    {
	return m_description;
    }

    public final GrinderProperties getParameters()
    {
	return m_parameters;
    }

    public Object invoke() throws GrinderException
    {
	return invokeTest(null);
    }

    protected final Object invokeTest(Object parameters)
	throws GrinderException
    {
	return TestRegistry.getInstance().invoke(m_registeredTest, parameters);
    }
}
