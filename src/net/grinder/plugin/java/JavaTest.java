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
import net.grinder.plugininterface.PluginTest;


/**
 * @author Philip Aston
 * @version $Revision$
 */ 
public class JavaTest extends PluginTest
{
    public JavaTest(int number, String description) throws GrinderException
    {
	super(JavaPlugin.class, number, description);
    }

    /**
     * Expose dispatch method to our package.
     */
    protected Object dispatch(Object parameters) throws GrinderException
    {
	return super.dispatch(parameters);
    }

    /**
     * We could have defined overloaded createProxy methdos that
     * take a PyInstance, PyFunction etc., and return decorator
     * PyObjects. There's no obvious way of doing this in a
     * polymorphic way, so we would be forced to have n factories,
     * n types of decorator, and probably run into identity
     * issues. Instead we lean on Jython and force it to give us
     * Java proxy which we then dynamically subclass with our own
     * type of PyJavaInstance.
     */
    public final Object createProxy(Object target)
    {
	return new TestPyJavaInstance(this, target);
    }
}

