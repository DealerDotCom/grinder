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
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.Enumeration;

public class GrindingServlet extends HttpServlet {

    /** want the timeout to be fairly generous so the punter can view
        his results, and then possibly re-run the test with different
        params. */
    private final static int TIMEOUT = 300;

    private final static int MAX_PROCS = 5;
    private final static int MAX_THREADS = 25;
    private final static int MAX_CYCLES = 50;

    private final static String[] JAVA_PROCESS = {
	"java", 
	"-classpath"};		
    private final static String[] GRINDER_PROCESS = {
	"net.grinder.Grinder"};
    private final static String CLASSPATH = "java:comp/env/grinder.classpath";
    private final static String CERTIFICATE = "java:comp/env/grinder.cert.file";
    private final static String PASSWORD = "java:comp/env/grinder.cert.password";

    private static final String GPROPS = "grinder.properties";
    private static final String SNIFFOUT = "httpsniffer.out";
    private static final String LOG_DIR = "log";

    private static final String SETUP_JSP = "/startgrnd.jsp";
    private static final String RESULTS_JSP = "/grndresults.jsp";
    private static final String ERROR_JSP = "/error.jsp";
    private static final String WAIT_JSP = "/wait.jsp";
    private static final String ERROR_TAG = "ErrorMsg";
    private static final String ACTION_TAG = "action";
    private static final String START_TAG = "grindme";
    private static final String CHECK_TAG = "check";
    private final static String OUTPUT_TAG = "ResultsDir";
    private final static String PROCESS_TAG = "GrinderProcess";
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

	ServletContext ctx = getServletContext();
	RequestDispatcher rd = ctx.getRequestDispatcher(SETUP_JSP);

	HttpSession session = request.getSession(false);
	if (session == null) {
	    // we got a problem - this should only be called after
	    // running the proxy.
	    session = request.getSession(true);
	    session.setAttribute(
		ERROR_TAG, "You need to have run the Proxy before running " +
		"the Grinder. Go to <a href=\"/index.jsp\">the start page</a> " +
		" to run the Proxy.");
	    rd = ctx.getRequestDispatcher(ERROR_JSP);
	} else {

	    // never time out
	    session.setMaxInactiveInterval(-1);
	    
	    String pValue = request.getParameter(ACTION_TAG);
	    
	    
	    if(pValue != null && pValue.equals(START_TAG)) {
		
		rd = ctx.getRequestDispatcher(WAIT_JSP);

		// basic default values - make them good and low so
		// some bozo who clicks "OK" without setting the
		// numbers doesn't shag the system
		int p = 1;
		int t = 1;
		int c = 1;

		try {
		    p = Integer.parseInt(request.getParameter("procs"));
		    p = p > MAX_PROCS ? MAX_PROCS : p;
		} catch (NumberFormatException e) {
		    // ignore the error and stick with the default
		}

		try {
		    t = Integer.parseInt(request.getParameter("threads"));
		    t = t > MAX_THREADS ? MAX_THREADS : t;
		} catch (NumberFormatException e) {
		    // ignore the error and stick with the default
		}

		try {
		    c = Integer.parseInt(request.getParameter("cycles"));
		    c = c > MAX_CYCLES ? MAX_CYCLES : c;
		} catch (NumberFormatException e) {
		    // ignore the error and stick with the default
		}

		try {
		    String dirname = (String)session.getAttribute(OUTPUT_TAG);
		    File workdir = new File(dirname);
		    
		    // create the logs directory
		    (new File(dirname + File.separator + LOG_DIR)).mkdir();
		    
		    // create the grinder.properties file
		    FileInputStream gpin = new FileInputStream(GPROPS);
		    FileOutputStream gpout = new FileOutputStream(
			dirname + File.separator + GPROPS);

		    copyBytes(gpin, gpout);
		    gpin.close();

		    InitialContext ictx = new InitialContext();
		    
		    Boolean isSecure = (Boolean)session.getAttribute(SECURE_TAG);
		    if(isSecure.booleanValue()) {
			gpout.write(
			    ("grinder.plugin="+
			     "net.grinder.plugin.http.HttpsPlugin" +
			     "\n").getBytes());

			// cert name and password are in the web.xml
			String certificate = (String)ictx.lookup(CERTIFICATE);
			String password = (String)ictx.lookup(PASSWORD);


			gpout.write(
			    ("grinder.plugin.parameter.clientCert=" +
			     certificate + "\n").getBytes());
			gpout.write(
			    ("grinder.plugin.parameter.clientCertPassword=" +
			     password + "\n").getBytes());
		    } else {
			gpout.write(
			    ("grinder.plugin="+
			     "net.grinder.plugin.http.HttpPlugin\n").getBytes());
		    }

		    gpout.write(("grinder.processes=" + p + "\n").getBytes());
		    gpout.write(("grinder.threads=" + t + "\n").getBytes());
		    gpout.write(("grinder.cycles=" + c + "\n").getBytes());
		    
		    gpin = new FileInputStream(
			dirname + File.separator + SNIFFOUT);
		    
		    copyBytes(gpin, gpout);
		    gpin.close();
		    gpout.close();

		    String classpath = (String)ictx.lookup(CLASSPATH);
		    String cmd[] = new String[JAVA_PROCESS.length + 1 + 
					     GRINDER_PROCESS.length];
		    int i = 0;
		    int n = JAVA_PROCESS.length;
		    System.arraycopy(JAVA_PROCESS, 0, cmd, i, n);
		    cmd[n] = classpath;
		    i = n + 1;
		    n = GRINDER_PROCESS.length;
		    System.arraycopy(GRINDER_PROCESS, 0, cmd, i, n);
		    
		    for(int j = 0; j < cmd.length; ++j) {
			System.out.print(cmd[j] + " ");
		    }
		    Process proc = Runtime.getRuntime().exec(cmd, null, workdir);

		    /*
		      InputStream err = proc.getErrorStream();
		      copyBytes(err, System.out);
		    */

		    session.setAttribute(PROCESS_TAG, proc);
		    
		} catch (NamingException e) {
		    e.printStackTrace();
		    session.setAttribute(
			ERROR_MSG_TAG, e.toString()
			);
		    
		    session.setMaxInactiveInterval(TIMEOUT);
		    rd = ctx.getRequestDispatcher(ERROR_JSP);
		    
		} catch (Throwable e) {
		    e.printStackTrace(response.getWriter());
		    throw new IOException(e.getMessage());
		}
		
	    } else if(pValue != null && pValue.equals(CHECK_TAG)) {
		// check whether the process is still valid
		boolean finished = true;
		try {
		    Process p = (Process)session.getAttribute(PROCESS_TAG);
		    int result = p.exitValue();
		} catch (IllegalThreadStateException e) {
		    // we're not done yet
		    finished = false;
		}

		// now we want to timeout
		if (finished) {
		    session.setMaxInactiveInterval(TIMEOUT);
		    rd = ctx.getRequestDispatcher(RESULTS_JSP);
		} else {
		    rd = ctx.getRequestDispatcher(WAIT_JSP);
		}
	    }
	    
	    try {
		rd.forward(request, response);
	    } catch (ServletException e) {
		e.printStackTrace(response.getWriter());
		throw new IOException(e.getMessage());
	    }
	}
    }

    private void copyBytes(InputStream in, OutputStream out) 
	throws IOException {

	BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	PrintWriter writer = new PrintWriter(out, true);
	String s = reader.readLine() + "\n";;
	while (s != null) {
	    writer.println(s);
	    s = reader.readLine();
	}
    }

}
    
