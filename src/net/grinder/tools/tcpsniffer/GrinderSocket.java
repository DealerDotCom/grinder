package net.grinder.tools.tcpsniffer;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/** there's s bug (about 3 years old!) in java sockets: close an input
    or output stream on a socket and the socket gets closed. as we
    need to have two threads connection in and out on two sockets,
    closing the streams on one will cause both sockets to close. not
    good. this class attempts to hack a way around it.  
*/

public class GrinderSocket extends Socket {
    private boolean m_inclosed = false;
    private boolean m_outclosed = false;
    private Socket m_socket = null;

    public GrinderSocket(Socket s) {
	m_socket = s;
    }

    public void writeOutput(byte[] buffer, int offset, int length) 
	throws IOException {
	m_socket.getOutputStream().write(buffer, offset, length);
	System.out.println("wrote \n" + new String(buffer) + "\non " + m_socket);
    }

    public int readInput(byte[] buffer, int offset, int length)  
	throws IOException {
	int bytesRead = m_socket.getInputStream().read(buffer, offset, length);
	System.out.println("read \n" + new String(buffer) + "\non " + m_socket);

	return bytesRead;
    }

    public void closeInput() throws IOException {
	m_inclosed = true;
	cleanup();
	System.out.println("closed input on " + m_socket);
    }

    public void closeOutput() throws IOException {
	m_outclosed = true;
	cleanup();
	System.out.println("closed output on " + m_socket);
    }

    private void cleanup() throws IOException {
	if(m_inclosed && m_outclosed) {
	    m_socket.getInputStream().close();
	    m_socket.getOutputStream().close();
	    m_socket.close();
	    System.out.println("closed " + m_socket);
	}
    }
}
