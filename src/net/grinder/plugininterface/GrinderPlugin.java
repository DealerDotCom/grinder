// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
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

import java.util.Set;

import net.grinder.common.GrinderProperties;


/**
 * This interface defines the callbacks that an individual Grinder
 * thread can make on a plugin.
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
public interface GrinderPlugin
{
    /**
     * This method is executed when the process starts. It is only
     * executed once.
     * @param processContext
     * @param testsFromPropertiesFile The tests defined in the
     * properties file. The plugin may or may not care about these.
     */
    public void initialize(PluginProcessContext processContext,
			   Set testsFromPropertiesFile)
	throws PluginException;

    /**
     * This method is called to create a handler for each thread.
     */
    public ThreadCallbacks createThreadCallbackHandler()
	throws PluginException;

    /**
     * Returns a Set of Tests to use. The plugin may chose to simply
     * return the set passed to {@link #initialize}.
     * @see #initialize
     */
    public Set getTests() throws PluginException;
}
