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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.grinder.plugininterface.PluginException;


/**
 * Closure to be invocated at a later time.
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
final class DelayedInvocation
{
    private final Object m_target;
    private final Method m_method;
    private final Object[] m_arguments;

    public DelayedInvocation(Object target, Method method,
			     Object[] arguments) {
	m_target = target;
	m_method = method;
	m_arguments = arguments;
    }

    public Object invoke() throws PluginException {
	try {
	    return m_target.getClass().getMethod(
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

