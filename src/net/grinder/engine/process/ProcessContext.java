// The Grinder
// Copyright (C) 2000  Paco Gomez
// Copyright (C) 2000  Philip Aston

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

package net.grinder.util;

import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.util.FilenameFactory;
import net.grinder.util.GrinderProperties;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class ProcessContextImplementation implements PluginProcessContext
{
    private final GrinderProperties m_pluginParameters;
    private final String m_hostIDString;
    private final String m_processIDString;

    private final FilenameFactory m_filenameFactory;

    protected ProcessContextImplementation(PluginProcessContext processContext,
					   String threadID)
    {
	m_pluginParameters = processContext.getPluginParameters();
	m_hostIDString = processContext.getHostIDString();
	m_processIDString = processContext.getProcessIDString();

	m_filenameFactory = new FilenameFactory(m_hostIDString,
						m_processIDString, threadID);
    }

    public ProcessContextImplementation(String hostIDString,
					String processIDString)
    {
	m_hostIDString = hostIDString;
	m_processIDString = processIDString;

	final GrinderProperties properties = GrinderProperties.getProperties();

	m_pluginParameters =
	    properties.getPropertySubset("grinder.plugin.parameter.");

	m_filenameFactory = new FilenameFactory(m_hostIDString,
						m_processIDString, null);
    }

    public String getHostIDString()
    {
	return m_hostIDString;
    }
    
    public String getProcessIDString()
    {
	return m_processIDString;
    }

    public FilenameFactory getFilenameFactory()
    {
	return m_filenameFactory;
    }

    public GrinderProperties getPluginParameters()
    {
	return m_pluginParameters;
    }

    public void logMessage(String message)
    {
	System.out.println(formatMessage(message));
    }

    public void logError(String message) 
    {
	System.err.println(formatMessage(message));

	final int summaryLength = 20;

	final String summary = 
	    message.length() > summaryLength ?
	    message.substring(0, summaryLength) + "..." : message;

	System.out.println(formatMessage("ERROR (\"" + summary +
					 "\"), see error log for details"));
    }

    protected String formatMessage(String message)
    {
	final StringBuffer buffer = new StringBuffer();

	buffer.append("Grinder Process (Host ");
	buffer.append(getHostIDString());
	buffer.append(" JVM ");
	buffer.append(getProcessIDString());
	buffer.append(") ");

	buffer.append(message);

	return buffer.toString();
    }
}

