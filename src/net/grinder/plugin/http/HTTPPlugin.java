// The Grinder
// Copyright (C) 2000  Paco Gomez

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
import net.grinder.engine.GrinderContext;
import net.grinder.engine.GrinderPlugin;

/**
 * Simple HTTP client benchmark.
 * 
 * @author <a href="mailto:paco.gomez@terra.com">Paco Gomez</a>.
 * @author <a href="mailto:paston@bea.com">Philip Aston</a>.
 * @author Copyright © 2000
 * @version 1.6.0
 */
public class HttpPlugin implements GrinderPlugin{

    /**
     * Private class that holds the data for a call.
     */
    private class CallData implements HttpRequestData {
	private String _urlString;
	private String _postString;
	private String _okString;
    
	public CallData(final int n) {

	    _urlString = _gc.paramAsString("url" + n);
	    _okString = _gc.paramAsString("ok" + n);

	    final String postFilename = _gc.paramAsString("post" + n);

	    if (postFilename != null) {
		try {
		    final FileReader in = new FileReader(postFilename);
		    final StringWriter writer = new StringWriter(512);
		    
		    char[] buffer = new char[512];
		    int charsRead = 0;

		    while ((charsRead = in.read(buffer, 0, buffer.length)) > 0) {
			writer.write(buffer, 0, charsRead);
		    }
		
		    in.close();
		    writer.close();
		    _postString = writer.toString();
		}
		catch (IOException e) {
		    System.err.println("Could not read post data from " +
				       postFilename);

		    e.printStackTrace(System.err);
		}
	    }	    
	}

	public String getURLString() { return _urlString; }
	public String getContextURLString() { return null; }
	public String getPostString() { return _postString; }
	public String getOKString() { return _okString; }
    }

    /**HttpRequestData implementation that wraps a GET */
    private class GetRequestData implements HttpRequestData
    {
	public GetRequestData(String contextURLString, String urlString) {
	    _contextURLString = contextURLString;
	    _urlString = urlString;
	}

	public String getURLString() { return _urlString; } 
	public String getContextURLString() { return _contextURLString; }
	public String getPostString() { return null; }

	private String _contextURLString;
	private String _urlString;
    }

    /**
     * This method initializes the plug-in.
     *
     */    
    public void init(GrinderContext gc){
  	
	_gc = gc;

	_callData = new CallData[_maxUrls];
    
	for (int i=0; i<_maxUrls; i++){
	    _callData[i] = new CallData(i);
	}     
    
	_httpMsg = new HttpMsg(gc.paramAsBoolean("keepSession"));
	_logHtml = gc.paramAsBoolean("logHtml");
	_suckSrcURLs = gc.paramAsBoolean("suckSrcURLs");
    }   


    /**
     * This method processes the URLs.
     *
     */    
    protected void processUrl(int i){

	if (i < 0 || i >= _maxUrls) {
	    throw new IllegalArgumentException();
	}
  	
	if (i == 0) {	// First URL, reset cookie and cache if necessary.
	    _httpMsg.reset();      
	}
    
	try{
	    // Do the call.
	    final String page = _httpMsg.sendRequest(_callData[i]);

	    if (_logHtml) {
		System.out.print(page);
	    }


	    final String okString = _callData[i].getOKString();

	    if (okString != null && page.indexOf(okString) == -1){
		final String fileName =
		    System.getProperty("grinder.logDir") + "/page-" +
		    System.getProperty("grinder.hostId", "0") + "-" + 
		    System.getProperty("grinder.jvmId", "0") + "-" +
		    _gc.paramAsString("grinder.threadId") + "-" +
		    _currentIteration + "-" +
		    i + ".html";

		final BufferedWriter htmlFile =
		    new BufferedWriter(new FileWriter(fileName, false));
		htmlFile.write(page);
		htmlFile.close();

		throw new Exception("the 'ok' string ('" + okString +
				    "') was not found in the page received. A file ('" + 
				    fileName + "') with the received answer is created.");
	    }

	    if (_suckSrcURLs) {
		final Iterator urlIterator = findSrcURls(page).iterator();
		final String contextURLString = _callData[i].getURLString();

		while (urlIterator.hasNext()) {
		    String urlString = (String)urlIterator.next();

		    _httpMsg.sendRequest(new GetRequestData(contextURLString,
							    urlString));
		}
	    }
	}
	catch(Exception e){      	
	    System.err.println("<E> <HttpBmkWlsBook-proccessUrl> ["+
			       System.getProperty("grinder.hostId", "0") + "," + 
			       System.getProperty("grinder.jvmId", "0") + "," +
			       _gc.paramAsString("grinder.threadId") + "," +
			       _currentIteration + "," +
			       "url=" + i +
			       "]" +
			       " error: " + e
			       );                          
	    _gc.setErrorIteration(true);
	    _gc.setSkipIteration(true);
	    e.printStackTrace(System.err);
	}
	if (i==0){
	    _currentIteration++;      
	}
    }

    /** Crude search for subordinate URLs. Should match images and
     * sources. Argggh, I lose - Java is such a crok why no regexp
     * engine?
     */
    private List findSrcURls(final String page) 
    {
	List result = new ArrayList();

	final String srcString = "src";
	final int length = page.length();

	int i = 0;

	while (i < length) {
	    try {
		if ((i = page.indexOf(srcString, i)) == -1) {
		    break;	// No more "src" attributes.
		}

		i = skipWhiteSpace(page, i + srcString.length());

		if (page.charAt(i++) != '=') {
		    continue;
		}

		i = skipWhiteSpace(page, i);

		if (!matchesStringDelimiter(page.charAt(i++))) {
		    continue;
		}

		final int begin = i;
		
		while (!matchesStringDelimiter(page.charAt(i++))) {}

		final int end = i-1;
		
		if (end > begin) {
		    result.add(page.substring(begin, end));
		}
	    }
	    catch (IndexOutOfBoundsException e) {
	    }
	}	

	return result;
    }

    private int skipWhiteSpace(String s, int i) 
    {
	final int length = s.length();
	
	while (Character.isWhitespace(s.charAt(i))) {
	    if (++i >= length) {
		i = -1;
		break;
	    }
	}

	return i;
    }

    private boolean matchesStringDelimiter(char c)
    {
	return c == '\'' || c == '"';
    }
    
    public void end(){
    }
  
    public void url0(){processUrl(0);}
    public void url1(){processUrl(1);}
    public void url2(){processUrl(2);}
    public void url3(){processUrl(3);}
    public void url4(){processUrl(4);}
    public void url5(){processUrl(5);}
    public void url6(){processUrl(6);}
    public void url7(){processUrl(7);}
    public void url8(){processUrl(8);}
    public void url9(){processUrl(9);}
    public void url10(){processUrl(10);}
    public void url11(){processUrl(11);}
    public void url12(){processUrl(12);}
    public void url13(){processUrl(13);}
    public void url14(){processUrl(14);}
    public void url15(){processUrl(15);}
    public void url16(){processUrl(16);}
    public void url17(){processUrl(17);}
    public void url18(){processUrl(18);}
    public void url19(){processUrl(19);}
    public void url20(){processUrl(20);}
    public void url21(){processUrl(21);}
    public void url22(){processUrl(22);}
    public void url23(){processUrl(23);}
    public void url24(){processUrl(24);}
    public void url25(){processUrl(25);}
    public void url26(){processUrl(26);}
    public void url27(){processUrl(27);}
    public void url28(){processUrl(28);}
    public void url29(){processUrl(29);}
    public void url30(){processUrl(30);}
    public void url31(){processUrl(31);}
    public void url32(){processUrl(32);}
    public void url33(){processUrl(33);}
    public void url34(){processUrl(34);}
    public void url35(){processUrl(35);}
    public void url36(){processUrl(36);}
    public void url37(){processUrl(37);}
    public void url38(){processUrl(38);}
    public void url39(){processUrl(39);}
    public void url40(){processUrl(40);}
    public void url41(){processUrl(41);}
    public void url42(){processUrl(42);}
    public void url43(){processUrl(43);}
    public void url44(){processUrl(44);}
    public void url45(){processUrl(45);}
    public void url46(){processUrl(46);}
    public void url47(){processUrl(47);}
    public void url48(){processUrl(48);}
    public void url49(){processUrl(49);}

    private GrinderContext _gc = null;  
    private CallData[] _callData = null;
    private final int _maxUrls = 50;
    private boolean _logHtml = true;
    private boolean _suckSrcURLs;
    private HttpMsg _httpMsg = null;
    private int _currentIteration = 0; // How many times we've done all the URL's
}
