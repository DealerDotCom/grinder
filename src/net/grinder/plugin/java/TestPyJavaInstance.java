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

import net.grinder.plugininterface.PluginException;

import org.python.core.PyJavaInstance;
import org.python.core.PyObject;


/**
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
abstract class TestPyJavaInstance extends PyJavaInstance
{
    private final PyObject m_test;
    
    public TestPyJavaInstance(PyObject test, Object proxy)
    {
        super(proxy);

	m_test = test;
    }

    protected PyObject ifindlocal(String name) {
        if (name == "__test__") { // Valid because name is interned.
	    return m_test;
	}

	return super.ifindlocal(name);
    }

    public PyObject invoke(final String name) 
    {
	return dispatch(
	    new Closure() {
		public PyObject invoke() {
		    return TestPyJavaInstance.super.invoke(name);
		}});
    }

    public PyObject invoke(final String name, final PyObject arg1) 
    {
	return dispatch(
	    new Closure() {
		public PyObject invoke() {
		    return TestPyJavaInstance.super.invoke(name, arg1);
		}});
    }

    public PyObject invoke(final String name, final PyObject arg1,
			   final PyObject arg2) 
    {
	return dispatch(
	    new Closure() {
		public PyObject invoke() {
		    return TestPyJavaInstance.super.invoke(name, arg1, arg2);
		}});
    }

    public PyObject invoke(final String name, final PyObject[] args) 
    {
	return dispatch(
	    new Closure() {
		public PyObject invoke() {
		    return TestPyJavaInstance.super.invoke(name, args);
		}});
    }

    public PyObject invoke(final String name, final PyObject[] args,
			   final String[] keywords) 
    {
	return dispatch(
	    new Closure() {
		public PyObject invoke() {
		    return TestPyJavaInstance.super.invoke(name, args,
							   keywords);
		}});
    }

    protected abstract PyObject dispatch(Closure closure);

    public interface Closure {
	public PyObject invoke();
    }
}

