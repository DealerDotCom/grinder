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
    private final Object m_target;
    private final Class m_targetClass;
    private final Class[] m_interfaces;
    private final InvocationHandler m_invocationHandler =
	new JavaTestInvocationHandler();

    public JavaTest(int number, String description, Object target)
	throws GrinderException
    {
	super(JavaPlugin.class, number, description);

	m_target = target;
	m_targetClass = target.getClass();

	final Class[] targetInterfaces = m_targetClass.getInterfaces();

	m_interfaces = new Class[targetInterfaces.length + 1];

	m_interfaces[0] = AdditionalProxyInterface.class;
	System.arraycopy(targetInterfaces, 0, m_interfaces, 1, 
			 targetInterfaces.length);
    }

    public Object invoke() throws PluginException
    {
	throw new PluginException("Invoke not supported");
    }

    Object getProxy() 
    {
	return Proxy.newProxyInstance(m_targetClass.getClassLoader(),
				      m_interfaces, m_invocationHandler);
    }

    private class JavaTestInvocationHandler implements InvocationHandler
    {
	public Object invoke(Object proxy, Method method, Object[] arguments)
	    throws Throwable
	{
	    final Method delegateMethod;

	    try {
		// Allow invocation of AdditionalProxyInterface methods.
		delegateMethod =
		    AdditionalProxyInterface.class.getMethod(
			method.getName(), method.getParameterTypes());

		return delegateMethod.invoke(new JavaTest.AdditionalMethods(),
					     arguments);
	    }
	    catch (NoSuchMethodException e) {
	    }

	    // Not an AdditionalProxyInterface method, pass
	    // DelayedInvocation through the engine. JavaPlugin will
	    // invoke on the target.
	    return JavaTest.this.invokeTest(
		new DelayedInvocation(method, arguments));
	}	
    }


    /**
     * Closure to be invoked at a later time.
     *
     * @author Philip Aston
     * @version $Revision$
     */ 
    final class DelayedInvocation
    {
	private final Method m_method;
	private final Object[] m_arguments;

	public DelayedInvocation(Method method, Object[] arguments) {
	    m_method = method;
	    m_arguments = arguments;
	}

	public Object invoke() throws PluginException {
	    try {
		return m_targetClass.getMethod(
		    m_method.getName(), m_method.getParameterTypes())
		    .invoke(m_target, m_arguments);
	    }
	    catch (IllegalAccessException e) {
		throw new PluginException("Invocation failed", e);
	    }
	    catch (InvocationTargetException e) {
		throw new PluginException("Invocation failed",
					  e.getTargetException());
	    }
	    catch (NoSuchMethodException e) {
		throw new PluginException("Invocation failed", e);
	    }
	}
    }

    private class AdditionalMethods implements AdditionalProxyInterface
    {
	public Test __getTestDetails__() 
	{
	    return JavaTest.this;
	}
    }
}
