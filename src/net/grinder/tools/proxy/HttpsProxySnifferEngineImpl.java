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

package net.grinder.tools.proxy;

import com.sun.net.ssl.KeyManager;
import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.X509TrustManager;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.InterruptedIOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

import net.grinder.tools.tcpsniffer.ConnectionDetails;
import net.grinder.tools.tcpsniffer.SnifferFilter;
import net.grinder.tools.tcpsniffer.SnifferEngine;
import net.grinder.tools.tcpsniffer.SSLSnifferEngineImpl;
import net.grinder.tools.tcpsniffer.NullFilter;
import net.grinder.tools.tcpsniffer.StreamThread;



/**
 

   Here's what this class does: it listens on the local port. When it
   gets a request in, it parses the CONNECT header to determine where
   it should be going. It then creates an SSLSnifferEngine and runs it
   in its own thread. It then creates two ordinary StreamThreads with
   NullFilters, one from some local port to the requested remote port
   and one from the local plain socket to the internal/local SSL
   port. It then sends a response to the local socket (the one
   connected to the browser) and goes back to waiting. 

 *
 
 * @author Paddy Spencer
 * @version $Revision$
 
 */
public class HttpsProxySnifferEngineImpl extends HttpProxySnifferEngineImpl
{
    private String m_keystoreName;
    private String m_keystorePassword;
    private int m_localSSLPort;

    private SSLSnifferEngineImpl m_sslEngine;

    public HttpsProxySnifferEngineImpl(SnifferFilter requestFilter,
				       SnifferFilter responseFilter,
				       String localHost,
				       int localPort, 
				       int localSSLPort,
				       int timeout,
				       boolean useColour,
				       String keystoreName, 
				       String keystorePassword)
        throws Exception
    {
        super(requestFilter, responseFilter, localHost, localPort, timeout, useColour);

	m_keystoreName = keystoreName;
	m_keystorePassword = keystorePassword;
	m_localSSLPort = localSSLPort;

	// set up an SSL sniffer on our local SSL port
	m_sslEngine = new SSLSnifferEngineImpl(requestFilter,
					       responseFilter,
					       m_localHost,
					       localSSLPort,
					       m_remoteHost, 
					       m_remotePort,
					       useColour,
					       keystoreName, 
					       keystorePassword);
	Thread t = new Thread(m_sslEngine);
	t.start();
    }



    public void run()
    {
        while (true) {
            try {
		m_serverSocket.setSoTimeout(m_timeout);
                final Socket socket = m_serverSocket.accept();

		// work out where we're supposed to be going
		grokDestination(socket);

		m_sslEngine.setRemoteDestination(m_remoteHost, m_remotePort);

		// now it's up and running (all the time-consuming SSL
		// stuff is in the constructor) we can connect to it
		Socket localSSL = new Socket("localhost", m_localSSLPort);

		// from socket to localSSLSocket
		new StreamThread(new ConnectionDetails("localhost",
						       socket.getPort(),
						       "localhost",
						       localSSL.getPort(),
						       false),
				 socket.getInputStream(),
				 localSSL.getOutputStream(),
				 new NullFilter(),
				 getColour(false));

		new StreamThread(new ConnectionDetails("localhost",
						       localSSL.getPort(),
						       "localhost",
						       socket.getPort(),
						       false),
				 localSSL.getInputStream(),
				 socket.getOutputStream(),
				 new NullFilter(),
				 getColour(true));

		// send 200 response to send to browser
		String ok = "HTTP/1. 200 OK\r\n";
		ok += "Host: " + m_remoteHost + ":" + m_remotePort + "\r\n";
		ok += "\r\n";
		socket.getOutputStream().write(ok.getBytes());

		// and go back to waiting....

            } catch (SocketException e) {
		// set so timeout failed
                e.printStackTrace(System.err);
	    } catch (InterruptedIOException e) {
		// socket timed out waiting for accept
		System.err.println("Proxy timed out waiting for request."
				   + " Exiting...");
		break;
            } catch(IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public void stop() {
	try {
	    m_serverSocket.close();
	} catch (IOException e) {
	    e.printStackTrace(System.err);
	}
    }

    protected void grokDestination(Socket socket) {

	final int BUFFER_SIZE = 65536;

	// we've got a connection which should be a CONNECT
	// request

        try {
	    byte[] buffer = new byte[BUFFER_SIZE];
	    int bytesRead = socket.getInputStream().read(buffer, 0, BUFFER_SIZE);
	    
	    if (bytesRead == -1) {
		// FIXME: this is an error
		System.err.println("SQUEAL!!! nothing to read!");
	    }
	    
	    final String line = new String(buffer, 0, bytesRead, "US-ASCII");

	    RE re = new RE("^CONNECT ([^:]+):(\\d+)");

	    if (re.match(line)) {

		m_remoteHost = re.getParen(1);
		m_remotePort = 443;
		try {
		    m_remotePort = Integer.parseInt(re.getParen(2));
		} catch (NumberFormatException e) {
		    // FIXME: for the ssl version, this is not ok -
		}
	    } else { 
		// FIXME: this is an error
		System.err.println("SQUAWK: no CONNECT header");
	    }

        } catch (SocketException e) {
	    e.printStackTrace();
        } catch (IOException e) {
	    e.printStackTrace();
	} catch (RESyntaxException e) {
	    e.printStackTrace();
	}

    }


}



