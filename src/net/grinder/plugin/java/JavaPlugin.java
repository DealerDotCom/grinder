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

import net.grinder.common.GrinderException;
import net.grinder.common.Test;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadCallbacks;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.PluginTest;
import net.grinder.script.ScriptPluginContext;


/**
 * Java plugin.
 * 
 * @author Philip Aston
 * @version $Revision$
 **/
public class JavaPlugin implements GrinderPlugin
{
    private PluginProcessContext m_processContext;
    private final ScriptPluginContext m_scriptPluginContext =
	new JavaPluginScriptPluginContext();

    public void initialize(PluginProcessContext processContext)
	throws PluginException
    {
	m_processContext = processContext;
    }

    public PluginThreadCallbacks createThreadCallbackHandler(
	PluginThreadContext threadContext)
	throws PluginException
    {
	return new JavaPluginThreadCallbacks(threadContext);
    }

    private class JavaPluginThreadCallbacks implements PluginThreadCallbacks
    {
	private final PluginThreadContext m_threadContext;

	public JavaPluginThreadCallbacks(PluginThreadContext threadContext)
	{
	    m_threadContext = threadContext;
	}

	public void beginRun() throws PluginException
	{
	}

	public Object invokeTest(Test test, Object parameters)
	    throws PluginException
	{
	    return ((TestPyJavaInstance.Invokeable)parameters).invoke();
	}

	public void endRun() throws PluginException
	{
	}
    }

    public final ScriptPluginContext getScriptPluginContext()
    {
	return m_scriptPluginContext;
    }

    public static final class JavaPluginScriptPluginContext
	implements ScriptPluginContext
    {
	// We could have defined overloaded createTests that take a
	// PyInstance, PyFunction etc., and return decorator
	// PyObjects. There's no obvious way of doing this in a
	// polymorphic way, so we would be forced to have n factories,
	// n types of decorator, and probably run into identity
	// issues. Instead we lean on Jython and force it to give us
	// Java proxy which we then dynamically subclass with our own
	// type of PyJavaInstance.
	public final Object createTest(int number, String description,
				       Object target)
	    throws GrinderException
	{
	    return new TestPyJavaInstance(new JavaTest(number, description),
					  target);
	}
    }

    /**
     * Represents an individual Java test. Scripts don't access this
     * directly, instead they get a proxy from <code>JavaPlugin</code>;
     * see {@link JavaPlugin#createTest}.
     *
     * @author Philip Aston
     * @version $Revision$
     */ 
    static class JavaTest extends PluginTest
    {
	public JavaTest(int number, String description) throws GrinderException
	{
	    super(JavaPlugin.class, number, description);
	}

	/**
	 * Expose dispatch method to our package.
	 */
	public Object dispatch(Object parameters) throws GrinderException
	{
	    return super.dispatch(parameters);
	}
    }
}
