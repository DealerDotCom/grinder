// The Grinder
// Copyright (C) 2001  Paco Gomez
// Copyright (C) 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

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
