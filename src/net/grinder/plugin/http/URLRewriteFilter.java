// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
// Copyright (C) 2000, 2001 Phil Dawes
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
import java.io.UnsupportedEncodingException;

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
public class URLRewriteFilter implements SnifferFilter
{
    public void handle(ConnectionDetails connectionDetails, byte[] buffer,
		       int bytesRead)
        throws IOException, RESyntaxException
    {
	final String ENCODING = "US-ASCII";

	// as Phil would say: hackety do dah
	final String protocol = connectionDetails.isSecure() ? "https" : "http";
	final String host = connectionDetails.getLocalHost();

	// not final because we do want to rewrite it if we find what
	// we're looking for.
	String string = new String(buffer, 0, bytesRead, ENCODING);
		
	// make two passes - one for href and one for target
	final RE re1 = new RE("(href)\\s*=\\s*(\"|')?" +
			      protocol + "://" + host + "(:\\d+)?/?",
			      RE.MATCH_CASEINDEPENDENT);

	final RE re2 = new RE("(target)\\s*=\\s*(\"|')?" +
			      protocol + "://" + host + "(:\\d+)?/?",
			      RE.MATCH_CASEINDEPENDENT);
		
	boolean href = re1.match(string);
	boolean target = re2.match(string);
	if (href || target){

	    if (href) {
		string = re1.subst(string, re1.getParen(1) + "=" +
				   re1.getParen(2) + "/", 
				   RE.REPLACE_ALL);
	    }
	    if (target) {
		string = re2.subst(string, re2.getParen(1) + "=" +
				   re2.getParen(2) + "/", 
				   RE.REPLACE_ALL);
	    }
			
	    try {
		// to avoid the end of the original file still being
		// at the end of the buffer, we make a tmpbuffer of
		// the same size as the original, copy the rewritten
		// buffer into it (which should leave the end of it
		// still null) and then copy the whole thing over the
		// original, which has the same size.(This relies on
		// the VM to initialise arrays and objects and stuff
		// to null, which it does.)
				
		byte[] tmpbuffer = new byte[bytesRead];
		byte[] bytes = string.getBytes(ENCODING);
				
		System.arraycopy(bytes, 0, tmpbuffer, 0, bytes.length);
		System.arraycopy(tmpbuffer, 0, buffer, 0, bytesRead);
			
			
		// none of these exceptions should occur, as we've
		// ensured that the arrays we're copying are of the
		// right sizes and aren't null.
	    } catch (IndexOutOfBoundsException e) {
		e.printStackTrace();
	    } catch (ArrayStoreException e) {
		e.printStackTrace();
	    } catch (NullPointerException e) {
		e.printStackTrace();
	    }
		
	} else {
	    // nothing matched so no need to do anything.
	}
    }

    public void connectionOpened(ConnectionDetails connectionDetails)
    {
    }

    public void connectionClosed(ConnectionDetails connectionDetails)
    {
    }
}



