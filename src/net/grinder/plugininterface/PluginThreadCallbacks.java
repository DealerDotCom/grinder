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

import net.grinder.common.Test;


/**
 * This interface defines the callbacks that an individual Grinder
 * thread can make on a plugin.
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
public interface PluginThreadCallbacks
{
    /**
     * This method is executed when the thread starts. It is only
     * executed once per thread.
     * @param pluginThreadContext Thread information. {@link
     * PluginThreadContext} implements {@link Logger} but for
     * efficiency the implementation isn't synchronised. Consequently
     * you should only call this object using the thread that which
     * the engine uses to invoke the {@link ThreadCallbacks}.
     */
    public void initialize(PluginThreadContext pluginThreadContext)
	throws PluginException;
    
    /**
     * This method is executed at the beginning of every run.
     **/
    public void beginRun() throws PluginException;

    /**
     * This is called for each test.
     **/
    public boolean doTest(Test testDefinition) throws PluginException;
    
    /**
     * This method is executed at the end of every run.
     **/  
    public void endRun() throws PluginException;
}
