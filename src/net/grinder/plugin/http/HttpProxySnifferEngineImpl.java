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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

import net.grinder.tools.tcpsniffer.ConnectionDetails;
import net.grinder.tools.tcpsniffer.SnifferFilter;
import net.grinder.tools.tcpsniffer.SnifferEngine;

/**
 *
 * @author Paddy Spencer
 * @version $Revision$
 */
public class HttpProxySnifferEngineImpl implements SnifferEngine
{
    private final SnifferFilter m_requestFilter;
    private final SnifferFilter m_responseFilter;
    private ServerSocket m_serverSocket = null;

    public HttpProxySnifferEngineImpl(SnifferFilter requestFilter,
				      SnifferFilter responseFilter,
				      int localPort)
        throws Exception
    {
        this(requestFilter, responseFilter);
        
        m_serverSocket = new ServerSocket(localPort);
    }

    protected HttpProxySnifferEngineImpl(SnifferFilter requestFilter,
					 SnifferFilter responseFilter) 
    {
        m_requestFilter = requestFilter;
        m_responseFilter = responseFilter;
    }

    protected void setServerSocket(ServerSocket serverSocket) 
    {
        m_serverSocket = serverSocket;
    }

    public void run()
    {
        while (true) {
            try {
                final Socket localSocket = m_serverSocket.accept();

		// create a new "upstream" thread on the socket.  this
		// thread will parse the input and then create a
		// second, "downstream" thread to handle the
		// response. So the thread needs three things from the
		// engine: the local socket, the request filter and
		// the response filter; everything else it can do for
		// itself.

		new UpStreamThread(localSocket, m_requestFilter, m_responseFilter);

            }
            catch(IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }
}


/**
   class to handle upstream requests from browser to remote server
*/
class UpStreamThread implements Runnable
{
    private final static int BUFFER_SIZE=65536;
    private final Socket m_socket;

    private final SnifferFilter m_requestFilter;
    private final SnifferFilter m_responseFilter;
	
    // if we make this final, the compiler complains that it might not
    // have been initialised in the ctor; however, if we assign it in
    // the catch block (when we know it won't have been initialised as
    // the call to getInputStream() will have thrown an exception) it
    // says "variable m_in might already have been assigned to" !!!
    private InputStream m_in;
	
    // not final as we will need to create new ones for each new
    // host:port combo
    private DownStreamThread m_down;
    private ConnectionDetails m_connectionDetails;
    private OutputStream m_out;


    public UpStreamThread(Socket socket,
			  SnifferFilter requestFilter, 
			  SnifferFilter responseFilter) {

	m_socket = socket;
        m_requestFilter = requestFilter;
	m_responseFilter = responseFilter;;

	try {
	    m_in = m_socket.getInputStream();
	    m_connectionDetails = new ConnectionDetails("localhost", 
							socket.getPort(),
							"unset",
							-1,
							false);
			
	    final Thread t = new Thread(this, m_connectionDetails.getDescription());
	    // System.out.println("Starting UpStreamThread " + t.getName());
	    t.start();

	} catch (IOException e) {
	    // if we can't get the input stream, there's no point in
	    // continuing...
	    e.printStackTrace();
	}
    }

    public void run()
    {
        try {
            while (true) {
		byte[] buffer = new byte[BUFFER_SIZE];

                int bytesRead = m_in.read(buffer, 0, BUFFER_SIZE);

                if (bytesRead == -1) {
                    break;
                } else {
		    // System.out.println("read " + bytesRead + " upstream");
		}

		byte[] bytes = grokDestination(buffer, bytesRead);

                m_requestFilter.handle(m_connectionDetails, bytes, bytes.length);
                m_out.write(bytes, 0, bytes.length);
            }
	    // System.out.println("closing upstream socket");
	    m_in.close();
	    m_out.close();
	    m_socket.close();
        }
        catch (SocketException e) {
            // Be silent about SocketExceptions.
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    // reads the contents of the buffer, checks to see if it's a new
    // request and changes connection details and other stuff as
    // appropriate.
    protected byte[] grokDestination(byte[] buffer, int bytesRead) {

	byte[] returnBytes = null;

	try {

	    final String line = new String(buffer, 0, bytesRead, "US-ASCII");

	    RE re = new RE("^([:upper:]+) http://([^/:]+):?(\\d*)/");

	    if (re.match(line)) {

		// got a request...

		String remoteHost = re.getParen(2);

		int remotePort = 80; 
		try {
		    remotePort = Integer.parseInt(re.getParen(3));
		} catch (NumberFormatException e) {
		    // that's ok - no port was given, so getParen(3)
		    // was null. It doesn't matter as we're happy with
		    // the default of 80.
		}

		// System.out.println("\nrequest for " + remoteHost + ":" + remotePort + "\n");

		// now the fun starts...

		// explicitly close existing output stream as we
		// don't want it anymore; until Sun fix things
		// this will also close the current remote socket,
		// but we don't care about that at the moment.  we
		// catch the exception here because we don't want
		// to miss the next few bits.
		if (m_out != null) {
		    try {
			m_out.close();
		    } catch (IOException e) {
			System.err.println("Exception closing remote output: " 
					   + e.getMessage() 
					   + "\n\tThis shouldn't matter.");
		    }
		}
				
		m_connectionDetails.setRemoteHost(remoteHost);
		m_connectionDetails.setRemotePort(remotePort);
				
		// dunno if anyone cares what our thread's called...
		Thread.currentThread().setName(m_connectionDetails.getDescription());
				
		Socket remoteSocket = new Socket(remoteHost, remotePort);
		m_out = remoteSocket.getOutputStream();
				
		new DownStreamThread(new ConnectionDetails(remoteHost,
							   remotePort,
							   "localhost",
							   m_socket.getPort(),
							   false),
				     remoteSocket.getInputStream(),
				     m_socket.getOutputStream(),
				     m_responseFilter);
				
		// lastly need to take the http://host:port bit out of the
		// method line, as the server's not expecting it to be
		// there.
		returnBytes = re.subst(line, re.getParen(1) + " /").getBytes();
	    }

	} catch (SocketException e) {
	    // be silent about SocketExceptions
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (RESyntaxException e) {
	    e.printStackTrace();
	}

	return returnBytes;
    }

}


/** class for handling "downstream" responses - identical to
    net.grinder.tools.tcpsniffer.StreamThread.
*/
class DownStreamThread implements Runnable
{
    private final static int BUFFER_SIZE=65536;

    private final ConnectionDetails m_connectionDetails;
    private final InputStream m_in;
    private final OutputStream m_out;
    private final SnifferFilter m_filter;

    public DownStreamThread(ConnectionDetails connectionDetails,
			    InputStream in, OutputStream out,
			    SnifferFilter filter)
    {
        m_connectionDetails = connectionDetails;
        m_in = in;
        m_out = out;
        m_filter = filter;
            
        final Thread t = new Thread(this, m_connectionDetails.getDescription());
	// System.out.println("Starting DownStreamThread " + t.getName());
        t.start();
    }
        
    public void run()
    {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                int bytesRead = m_in.read(buffer, 0, BUFFER_SIZE);

                if (bytesRead == -1) {
                    break;
                }

                m_filter.handle(m_connectionDetails, buffer, bytesRead);

                // and write in out
                m_out.write(buffer, 0, bytesRead);
            }
	    // System.out.println("closing downstream socket");
	    m_in.close();
			
	    // we can't close the output stream or else it'll close
	    // the socket for the upstream thread ans seriously screw
	    // us up.
	    // m_out.close();
        }
        catch (SocketException e) {
            // Be silent about SocketExceptions.
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
