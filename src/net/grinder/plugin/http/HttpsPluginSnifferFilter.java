// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2001 Paddy Spencer
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

package net.grinder.plugin.http;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

import net.grinder.tools.tcpsniffer.ConnectionDetails;
import net.grinder.tools.tcpsniffer.EchoFilter;
import net.grinder.tools.tcpsniffer.SnifferFilter;


/**
 *
 * @author Paddy Spencer
 * @version $Revision$
 */
public class HttpsPluginSnifferFilter extends HttpPluginSnifferFilter 
{
    public HttpsPluginSnifferFilter() throws RESyntaxException
    {
	super();
    }
    
    /**
     * we need to gather all the headers together to make sure that
     * we've reached the end of it. (In fact this will contain more
     * than just the headers, as it will also contain the start of any
     * post data.)
     */

    private String m_requestHeader = "";

    protected void addToEntityData(String request,
				 SessionState sessionState,
				 boolean thisMessageHasPOSTHeader)
	throws IOException, RESyntaxException
    {

	if (!sessionState.isFinishedHeaders()){
	    m_requestHeader += request;
	}

	// Look for the content length in the header. Probably should
	// assert that we haven't already set the content length nor
	// added any entity data.
	final RE contentLengthExpession = getContentLengthExpression();

	if (contentLengthExpession.match(request)) {
	    final int length =
		Integer.parseInt(contentLengthExpession.getParen(1));
	    
	    sessionState.setContentLength(length);
	}

	// look for the separator in the header as a whole
	final RE separatorExpession = getSeparatorExpression();
	
	if (separatorExpession.match(m_requestHeader)) {
	    
	    sessionState.setFinishedHeaders(true);
	}
	
	// don't bother doing this unless we're in the body
	if (sessionState.isFinishedHeaders()) {

	    // Find and add the data.(we need to look for different
	    // things if this request is all in one lump.
	    final RE messageBodyExpression = 
		getMessageBodyExpression(thisMessageHasPOSTHeader);
	    
	    if (messageBodyExpression.match(request)) {
		
		sessionState.addEntityData(messageBodyExpression.getParen(1));
		
		final int contentLength = sessionState.getContentLength();
	    
		// We flush our entity data output now if either
		//    1. No Content-Length header was specified and this
		//    body belonged to the same message as the POST method.
		// or
		//    2. We've reached or exceeded the specified Content-Length.
		if ((contentLength == -1 && thisMessageHasPOSTHeader) ||
		    contentLength != -1 &&
		    (sessionState.getEntityDataLength() >= contentLength)) {
		    
		    outputEntityData(sessionState);
		    
		    // finished shoving stuff out, so back to the headers we go.
		    m_requestHeader = "";
		    sessionState.setFinishedHeaders(false);
		}
	    }
	}
    }

    /**
     * Regexp is not synchronised, so for now compile new objects
     * every time. If it becomes a bottleneck, the "get*Expression
     * methods should be implemented with object pools.
    */
    protected RE getMessageBodyExpression(boolean withPOSTHeader) throws RESyntaxException
    {
	// what we look for depends on whether we get it all as one
	// lump or not. Which, in turn, depends on which browser we're
	// using and how they split the request up.
	return (withPOSTHeader ?
		new RE("\r\n\r\n(.*)") :
		new RE("\n*^(.*)", RE.MATCH_MULTILINE));
    }

    /**
     * Regexp is not synchronised, so for now compile new objects
     * every time. If it becomes a bottleneck, the "get*Expression
     * methods should be implemented with object pools.
    */
    protected RE getSeparatorExpression() throws RESyntaxException
    {
	return new RE("\r\n\r\n", RE.MATCH_MULTILINE);
    }

}
