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

package net.grinder.plugin.http;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import net.grinder.plugininterface.GrinderContext;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.util.FilenameFactory;
import net.grinder.util.GrinderException;
import net.grinder.util.GrinderProperties;


/**
 * Simple HTTP client benchmark.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public class HttpPlugin implements GrinderPlugin {

    /**
     * Inner class that holds the data for a call.
     */
    protected class CallData implements HttpRequestData
    {
	private String m_urlString;
	private String m_postString;
	private String m_okString;
    
	public CallData(final int n)
	{
	    m_urlString = m_parameters.getProperty("url" + n, null);
	    m_okString = m_parameters.getProperty("ok" + n, null);

	    final String postFilename = m_parameters.getProperty("post" + n,
								 null);

	    if (postFilename != null) {
		try {
		    final FileReader in = new FileReader(postFilename);
		    final StringWriter writer = new StringWriter(512);
		    
		    char[] buffer = new char[512];
		    int charsRead = 0;

		    while ((charsRead = in.read(buffer, 0, buffer.length)) > 0)
		    {
			writer.write(buffer, 0, charsRead);
		    }
		
		    in.close();
		    writer.close();
		    m_postString = writer.toString();
		}
		catch (IOException e) {
		    System.err.println("Could not read post data from " +
				       postFilename);

		    e.printStackTrace(System.err);
		}
	    }	    
	}

	public String getURLString() { return m_urlString; }
	public String getContextURLString() { return null; }
	public String getPostString() { return m_postString; }
	public String getOKString() { return m_okString; }

	protected void setURLString(String s) { m_urlString = s; }
	protected void setPostString(String s) { m_postString = s; }
	protected void setOKString(String s) { m_okString = s; }
    }

    /**
     * This method initializes the plug-in.
     *
     */    
    public void initialize(GrinderContext grinderContext)
	throws PluginException
    {
	m_grinderContext = grinderContext;
	m_parameters = grinderContext.getParameters();
	m_filenameFactory = grinderContext.getFilenameFactory();

	m_callData = new CallData[m_maxURLs];
    
	for (int i=0; i<m_maxURLs; i++){
	    m_callData[i] = createCallData(grinderContext, i);
	}     
    
	m_httpMsg = new HttpMsg(m_parameters.getBoolean("keepSession", false),
				m_parameters.getBoolean("followRedirects", false));
	m_logHTML = m_parameters.getBoolean("logHTML", false);
    }

    /**
     * Give derived classes a chance to be interesting.
     */
    protected CallData createCallData(GrinderContext grinderContext,
				      int urlNumber)
    {
	return new CallData(urlNumber);
    }
    

    /**
     * This method processes the URLs.
     *
     */    
    protected boolean processUrl(int i) throws Exception {

	if (i < 0 || i >= m_maxURLs) {
	    throw new PluginException("Invalid URL: " + i);
	}
    
	// Do the call.
	String page = null;

	m_grinderContext.startTimer();

	try {
	    page = m_httpMsg.sendRequest(m_callData[i]);
	}
	finally {
	    m_grinderContext.stopTimer();
	}

	final String okString = m_callData[i].getOKString();

	final boolean error =
	    okString != null  && page.indexOf(okString) == -1;
	
	if (m_logHTML || error) {
	    final String filename =
		m_filenameFactory.createFilename("page",
						 "_" + m_currentIteration +
						 "_" + i + ".html");
	    final BufferedWriter htmlFile =
		new BufferedWriter(new FileWriter(filename, false));
	    htmlFile.write(page);
	    htmlFile.close();

	    if (error) {
		System.err.println("The 'ok' string ('" + okString +
				   "') was not found in the page received.");
		System.err.println("The output has been written to '" +
				   filename + "'");
	    }
	}

	return !error;
    }

    public void beginCycle() throws PluginException {
	// Reset cookie if necessary.
	m_httpMsg.reset();      
    }

    public void endCycle()throws PluginException {
	m_currentIteration++;
    }

    public boolean url0() throws Exception { return processUrl(0); }
    public boolean url1() throws Exception { return processUrl(1); }
    public boolean url2() throws Exception { return processUrl(2); }
    public boolean url3() throws Exception { return processUrl(3); }
    public boolean url4() throws Exception { return processUrl(4); }
    public boolean url5() throws Exception { return processUrl(5); }
    public boolean url6() throws Exception { return processUrl(6); }
    public boolean url7() throws Exception { return processUrl(7); }
    public boolean url8() throws Exception { return processUrl(8); }
    public boolean url9() throws Exception { return processUrl(9); }
    public boolean url10() throws Exception { return processUrl(10); }
    public boolean url11() throws Exception { return processUrl(11); }
    public boolean url12() throws Exception { return processUrl(12); }
    public boolean url13() throws Exception { return processUrl(13); }
    public boolean url14() throws Exception { return processUrl(14); }
    public boolean url15() throws Exception { return processUrl(15); }
    public boolean url16() throws Exception { return processUrl(16); }
    public boolean url17() throws Exception { return processUrl(17); }
    public boolean url18() throws Exception { return processUrl(18); }
    public boolean url19() throws Exception { return processUrl(19); }
    public boolean url20() throws Exception { return processUrl(20); }
    public boolean url21() throws Exception { return processUrl(21); }
    public boolean url22() throws Exception { return processUrl(22); }
    public boolean url23() throws Exception { return processUrl(23); }
    public boolean url24() throws Exception { return processUrl(24); }
    public boolean url25() throws Exception { return processUrl(25); }
    public boolean url26() throws Exception { return processUrl(26); }
    public boolean url27() throws Exception { return processUrl(27); }
    public boolean url28() throws Exception { return processUrl(28); }
    public boolean url29() throws Exception { return processUrl(29); }
    public boolean url30() throws Exception { return processUrl(30); }
    public boolean url31() throws Exception { return processUrl(31); }
    public boolean url32() throws Exception { return processUrl(32); }
    public boolean url33() throws Exception { return processUrl(33); }
    public boolean url34() throws Exception { return processUrl(34); }
    public boolean url35() throws Exception { return processUrl(35); }
    public boolean url36() throws Exception { return processUrl(36); }
    public boolean url37() throws Exception { return processUrl(37); }
    public boolean url38() throws Exception { return processUrl(38); }
    public boolean url39() throws Exception { return processUrl(39); }
    public boolean url40() throws Exception { return processUrl(40); }
    public boolean url41() throws Exception { return processUrl(41); }
    public boolean url42() throws Exception { return processUrl(42); }
    public boolean url43() throws Exception { return processUrl(43); }
    public boolean url44() throws Exception { return processUrl(44); }
    public boolean url45() throws Exception { return processUrl(45); }
    public boolean url46() throws Exception { return processUrl(46); }
    public boolean url47() throws Exception { return processUrl(47); }
    public boolean url48() throws Exception { return processUrl(48); }
    public boolean url49() throws Exception { return processUrl(49); }

    private GrinderContext m_grinderContext = null;
    private GrinderProperties m_parameters = null;
    private FilenameFactory m_filenameFactory = null;
    private CallData[] m_callData = null;
    private final int m_maxURLs = 50;
    private boolean m_logHTML = true;
    private HttpMsg m_httpMsg = null;
    private int m_currentIteration = 0; // How many times we've done all the URL's
}
