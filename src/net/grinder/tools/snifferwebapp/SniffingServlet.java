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

package net.grinder.tools.snifferwebapp;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;

import java.util.Random;
import java.util.Enumeration;

import net.grinder.tools.snifferwebapp.util.PortManager;
import net.grinder.tools.snifferwebapp.util.NoFreePortException;

public class SniffingServlet extends HttpServlet {

    /** 
	used to make unique tmp directories; using Random and then
	incrementing it means that, for the lifetime of the current
	JVM, this will not reuse any filenames (or at least, not for a
	long time...) 
    */
    private static int m_count = (new Random()).nextInt() & 0xffff;;

    private final static int TIMEOUT = 30;

    private final static String SETUP_JSP = "/index.jsp";
    private final static String GO_JSP = "/start.jsp";
    private final static String RESULT_JSP = "/results.jsp";
    private final static String ERROR_JSP = "/error.jsp";

    private final static String PROXYTIMEOUT = "java:comp/env/sniffer.timeout";
    private final static String CLASSPATH = "java:comp/env/grinder.classpath";
    private final static String CERTIFICATE = "java:comp/env/grinder.cert.file";
    private final static String PASSWORD = "java:comp/env/grinder.cert.password";

    private final static String[] JAVA_PROCESS = {
	"java", 
	"-classpath"};		
    private final static String[] SNIFFER_PROCESS = {
	"net.grinder.TCPSniffer",
	"-httpPluginFilter",
	"-proxy",
	"-output",
	"httpsniffer",
	"-localPort" };

    private final static String ACTION_TAG = "action";
    private final static String START_TAG = "start";
    private final static String STOP_TAG = "stop";
    private final static String START_URL_TAG = "StartURL";
    private final static String PROCESS_TAG = "SnifferProcess";
    private final static String OUTPUT_TAG = "ResultsDir";
    private final static String PORT_TAG = "port";
    private final static String ERROR_MSG_TAG = "ErrorMsg";
    private final static String SECURE_TAG = "IsSecure";


    public void doGet(HttpServletRequest request, 
		      HttpServletResponse response)
	throws IOException {
	processRequest(request, response);
    }

    public void doPost(HttpServletRequest request, 
		       HttpServletResponse response)
	throws IOException {
	processRequest(request, response);
    }


    private void processRequest(HttpServletRequest request, 
				HttpServletResponse response)
	throws IOException {

	response.setContentType("text/html");

	HttpSession session = request.getSession(true);
	// never time out
	session.setMaxInactiveInterval(-1);

	String pValue = request.getParameter(ACTION_TAG);

	ServletContext ctx = getServletContext();
	RequestDispatcher rd = ctx.getRequestDispatcher(SETUP_JSP);

	if(pValue != null && pValue.equals(START_TAG)) {
	    
	    String url = request.getParameter(START_URL_TAG);
	    
	    // Kludge alert!
	    boolean isSecure = false;
	    if (url.startsWith("https://")) {
		isSecure = true;
	    } else if (!url.startsWith("http")) {
		url = "http://" + url;
	    }
	    
	    System.out.println(isSecure ? "secure" : "insecure");
	    
	    session.setAttribute(START_URL_TAG, url);
	    session.setAttribute(SECURE_TAG, new Boolean(isSecure));
	    
	    // we reset this if anythings goes awry
	    rd = ctx.getRequestDispatcher(GO_JSP);

	    try {
		File workdir = null;
		try {
		    
		    workdir  = new File("sniffer." + (m_count++) + ".tmp");
		    workdir.deleteOnExit();
		    boolean result = workdir.mkdir();
		    if (result) {
			session.setAttribute(OUTPUT_TAG, workdir.getName());
		    } else {
			session.setAttribute(OUTPUT_TAG, ".");
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
		
		String[] cmd = commandStrings(session);
		
		Process p = Runtime.getRuntime().exec(cmd, null, workdir);
		
		session.setAttribute(PROCESS_TAG, p);
	    
	    } catch (NoFreePortException e) {
		session.setAttribute(
		    ERROR_MSG_TAG,
		    "No ports are currently available, " +
		    "please try again in a few minutes."
		    );
	    
		session.setMaxInactiveInterval(TIMEOUT);
		rd = ctx.getRequestDispatcher(ERROR_JSP);
		
	    } catch (NamingException e) {
		e.printStackTrace();
		session.setAttribute(
		    ERROR_MSG_TAG, e.toString()
		    );
		
		session.setMaxInactiveInterval(TIMEOUT);
		rd = ctx.getRequestDispatcher(ERROR_JSP);
		
	    } catch (Throwable t) {
		t.printStackTrace(response.getWriter());
		throw new IOException(t.getMessage());
	    }
	    
	} else if(pValue != null && pValue.equals(STOP_TAG)) {
	    Process p = (Process)session.getAttribute(PROCESS_TAG);
	    p.destroy();
	    
	    session.removeAttribute(PROCESS_TAG);
	    
	    // release the port
	    
	    Integer port = (Integer)session.getAttribute(PORT_TAG);
	    session.removeAttribute(PORT_TAG);
	    PortManager.getInstance().releasePort(port.intValue());
	    
	    // now we want to timeout - we set it not to timeout if we
	    // go on to the grinder runner
	    session.setMaxInactiveInterval(TIMEOUT);
	    rd = ctx.getRequestDispatcher(RESULT_JSP);
	}
	
	try {
	    rd.forward(request, response);
	} catch (ServletException e) {
	    e.printStackTrace(response.getWriter());
	    throw new IOException(e.getMessage());
	}
    }
    
    private String[] commandStrings(HttpSession session) 
	throws NoFreePortException, NamingException {

	boolean isSecure = 
	    ((Boolean)session.getAttribute(SECURE_TAG)).booleanValue();

	InitialContext ctx = new InitialContext();
	
	Integer timeout = (Integer)ctx.lookup(PROXYTIMEOUT);
	String classpath = (String)ctx.lookup(CLASSPATH);
	
	// additional cmd elements are classpath and local port extra
	// args are: port, "-timeout", timeout, with added "-ssl",
	// "-certificate", file, "-password" pw, "-localSSLPort" and
	// port for the secure proxy

	int extraArgs = isSecure ? 12: 3;
	
	String[] cmd = new String[JAVA_PROCESS.length + 1 + 
			SNIFFER_PROCESS.length + extraArgs];

	int i = 0;
	int n = JAVA_PROCESS.length;
	System.arraycopy(JAVA_PROCESS, 0, cmd, i, n);
	cmd[n] = classpath;
	i = n + 1;
	n = SNIFFER_PROCESS.length;
	System.arraycopy(SNIFFER_PROCESS, 0, cmd, i, n);
	i = JAVA_PROCESS.length + 1 + SNIFFER_PROCESS.length;
	
	int port = PortManager.getInstance().acquirePort();
	session.setAttribute(PORT_TAG, new Integer(port));
	
	cmd[i] = "" + port;
	
	// add proxy timeout
	cmd[++i] = "-timeout";
	cmd[++i] = "" + timeout;
	
	if (isSecure) {
	    int sslport = PortManager.getInstance().acquirePort();

	    // cert name and password are in the web.xml
	    String certificate = (String)ctx.lookup(CERTIFICATE);
	    String password = (String)ctx.lookup(PASSWORD);

	    cmd[++i] = "-ssl";
	    cmd[++i] = "-keyStore";
	    cmd[++i] = certificate;
	    cmd[++i] = "-keyStorePassword";
	    cmd[++i] = password;
	    cmd[++i] = "-keyStoreType";
	    cmd[++i] = "pkcs12";
	    cmd[++i] = "-localSSLPort";
	    cmd[++i] = "" + sslport;
	}

	for(int j = 0; j < cmd.length; ++j) {
	    System.out.print(cmd[j] + " ");
	}
	System.out.println("\n---------------------------------------------");
	return cmd;

    }
}

