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

package net.grinder.plugin.http.example;

import net.grinder.common.Test;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugin.http.StringBean;


/**
 * Example String Bean that implements StringBean.
 * 
 * @author Philip Aston
 * @version $Revision$
 */
public class ExampleStringBean2 implements StringBean
{
    private PluginThreadContext m_threadContext;
    private int m_count = 0;

    public void initialize(PluginProcessContext pluginProcessContext,
			   PluginThreadContext threadContext)
	throws PluginException
    {
	m_threadContext = threadContext;
	m_threadContext.logMessage("StringBean: initialize");
    }

    public void beginRun()
    {
	m_threadContext.logMessage("StringBean: beginRun");
	m_count = 0;
    }

    public void doTest(Test test)
    {
	m_threadContext.logMessage("StringBean: doTest");
    }

    public void endRun()
    {
	m_threadContext.logMessage("StringBean: endRun");
    }
    
    public String getCount()
    {
	return Integer.toString(m_count++);
    }

    public String getTime()
    {
	return Long.toString(System.currentTimeMillis());
    }

    public String getRun()
    {
	return Integer.toString(m_threadContext.getCurrentRunNumber());
    }
}
