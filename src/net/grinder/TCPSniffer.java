// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000, 2001, 2002 Philip Aston
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

package net.grinder;

import java.lang.reflect.Constructor;

import net.grinder.tools.tcpsniffer.EchoFilter;
import net.grinder.tools.tcpsniffer.NullFilter;
import net.grinder.tools.tcpsniffer.SnifferEngine;
import net.grinder.tools.tcpsniffer.SnifferEngineImpl;
import net.grinder.tools.tcpsniffer.SnifferFilter;


/**
 *
 * @author Phil Dawes
 * @author Philip Aston
 * @version $Revision$
 */
public class TCPSniffer
{
    public static final String INITIAL_TEST_PROPERTY =
	"TCPSniffer.initialTest";

    public static void main(String args[])
    {
	final TCPSniffer tcpSniffer = new TCPSniffer(args);
	tcpSniffer.run();
    }

    private Error barfUsage()
    {
	System.err.println(
	    "\n" +
	    "Usage: " +
	    "\n java " + TCPSniffer.class + " <options>" +
	    "\n" +
	    "\n Where options can include:" +
	    "\n" +
	    "\n   [-requestFilter <filter>]      Add request filter" +
	    "\n   [-responseFilter <filter>]     Add response filter" +
	    "\n   [-httpPluginFilter             See below" +
	    "\n     [-rewriteURLs]               See below" +
	    "\n     [-proxy]                     See below" +
	    "\n     [-initialTest <n>]           Number tests from n" +
	    "\n   ]" +
	    "\n   [-localHost <host name/ip>]    Default is localhost" +
	    "\n   [-localPort <port>]            Default is 8001" +
	    "\n   [-remoteHost <host name>]      Default is localhost" +
	    "\n   [-remotePort <port>]           Default is 7001" +
	    "\n   [-ssl                          Use SSL" +
	    "\n    [-certificate <PKCS12 file>   Optional client certificate" +
	    "\n     -password <password>]        Certificate keystore pass" +
	    "\n    [-localSSLPort <port>]        Local port for SSL sniffer" +
	    "\n   ]" +
	    "\n   [-colour]                      Be pretty on ANSI terminals" +
	    "\n" +
	    "\n   [-timeout]                     Sniffer engine timeout" +
	    "\n" +
	    "\n <filter> can be the name of a class that implements " +
	    "\n " + SnifferFilter.class.getName() + " or " +
	    "\n one of NONE, ECHO, or HTTP_PLUGIN. Default is ECHO." +
	    "\n" +
	    "\n -proxy means the sniffer will act as an http(s) proxy," +
	    "\n" +
	    "\n -httpPluginFilter is a synonym for" +
	    "\n   '-requestFilter HTTP_PLUGIN -responseFilter NONE', " +
	    "\n   an HTTPS plugin is used if a secure proxy is specified" +
	    "\n" +
	    "\n -rewriteURLs will cause absolute URLs to remoteHost in" +
	    "\n    pages from remoteHost to be rewritten as relative" +
	    "\n    URLs." +
	    "\n" +
	    "\n -timeout is how long (in seconds) the sniffer will wait" +
	    "\n    for a request before timing out and freeing the local" +
	    "\n    port." +
	    "\n"
	    );

	System.exit(1);

	return null;
    }

    private Error barfUsage(String s)
    {
	System.err.println("\n" + "Error: " + s);
	throw barfUsage();
    }

    private SnifferEngine m_snifferEngine = null;
    private final String SSL_ENGINE_CLASS =
	"net.grinder.tools.tcpsniffer.SSLSnifferEngineImpl";
    private final String HTTP_PROXY_ENGINE_CLASS =
	"net.grinder.tools.proxy.HttpProxySnifferEngineImpl";
    private final String HTTPS_PROXY_ENGINE_CLASS =
	"net.grinder.tools.proxy.HttpsProxySnifferEngineImpl";
    private final String HTTP_PLUGIN_FILTER_CLASS =
	"net.grinder.plugin.http.HttpPluginSnifferFilter";
    private final String HTTPS_PLUGIN_FILTER_CLASS =
	"net.grinder.plugin.http.HttpsPluginSnifferFilter";
    private final String URL_REWRITE_FILTER_CLASS =
	"net.grinder.plugin.http.URLRewriteFilter";

    private TCPSniffer(String args[])
    {
	// Default values.
	SnifferFilter requestFilter = new EchoFilter();
	SnifferFilter responseFilter = new EchoFilter();
	int localPort = 8001;
	String remoteHost = "localhost";
	String localHost = "localhost";
	int remotePort = 7001;
	boolean useSSL = false;
	String keystore = null;
	String keystorePassword = null;

	int localSSLPort = 9001;
	boolean rewriteURLs = false;
	boolean proxy = false;
	int initialTest = 0;

	int timeout = 0; 

	boolean useColour = false;

	try {
	    // Parse 1.
	    for (int i=0; i < args.length; i++)
	    {
		if (args[i].equals("-initialTest")) {
		    initialTest = Integer.parseInt(args[++i]);
		}
	    }

	    System.setProperty(INITIAL_TEST_PROPERTY,
			       Integer.toString(initialTest));

	    // Parse 2
	    for (int i=0; i<args.length; i++)
	    {
		if (args[i].equals("-requestFilter")) {
		    requestFilter = instantiateFilter(args[++i]);
		}
		else if (args[i].equals("-responseFilter")) {
		    responseFilter = instantiateFilter(args[++i]);
		}
		else if (args[i].equals("-httpPluginFilter")) {
		    requestFilter = httpPluginFilterInstance();
		    responseFilter = new NullFilter();
		}
		else if (args[i].equals("-localHost")) {
		    localHost = args[++i];
		}
		else if (args[i].equals("-localPort")) {
		    localPort = Integer.parseInt(args[++i]);
		}
		else if (args[i].equals("-remoteHost")) {
		    remoteHost = args[++i];
		}
		else if (args[i].equals("-remotePort")) {
		    remotePort = Integer.parseInt(args[++i]);
		}
		else if (args[i].equals("-ssl")) {
		    useSSL = true;
		}
		else if (args[i].equals("-certificate")) {
		    keystore = args[++i];
		}
		else if (args[i].equals("-password")) {
		    keystorePassword = args[++i];
		}
		else if (args[i].equals("-localSSLPort")) {
		    localSSLPort = Integer.parseInt(args[++i]);
		}
		else if (args[i].equals("-rewriteURLs")) {
		    rewriteURLs = true;
		}
		else if (args[i].equals("-proxy")) {
		    proxy = true;
		    rewriteURLs = false;
		}
		else if (args[i].equals("-timeout")) {
		    timeout = Integer.parseInt(args[++i])*1000;
		}
		else if (args[i].equals("-colour")) {
		    useColour = true;
		}
		else if (args[i].equals("-initialTest")) {
		    /* Already handled */
		    ++i;
		}
		else {
		    throw barfUsage();
		}
	    }
	}
	catch (Exception e) {
	    throw barfUsage();
	}

	if (proxy) {

	    if (useSSL) {
		// if we're doing a secure proxy, we need a slightly
		// different version of the http filter as the request
		// comes in differently.
		requestFilter = httpsPluginFilterInstance();
	    }

	    if (!filterIsHttpFilter(requestFilter)) {
		throw barfUsage("Specify HTTP_PLUGIN as the request filter " +
				"when using -proxy");
	    }
	}

	if (timeout < 0) {
	    throw barfUsage("Proxy timeout must be non-negative");
	}

	if (rewriteURLs) {
	    if (!filterIsHttpFilter(requestFilter)) {
		throw barfUsage("Specify HTTP_PLUGIN as the request filter " +
				"when using -rewriteURLs");
	    }
	    responseFilter = urlRewriteFilterInstance();
	}

	if (!useSSL) {
	    if (keystore != null || keystorePassword != null) {
		throw barfUsage("Keystore parameters only valid with '-ssl'");
	    }
	    // FIXME: add check for setting localSSLPort without SSL
	}
	else {
	    if ((keystore != null) ^ (keystorePassword != null)) {
		throw barfUsage(
		    "Specify both -keystore and -keystorePassword or neither");
	    }
	}

    

	final StringBuffer startMessage = new StringBuffer();

	startMessage.append(
	    "Initialising " + (useSSL ? "SSL" : "standard") +
	    " sniffer engine with the parameters:" +
	    "\n   Request filter:  " + requestFilter.getClass().getName() +
	    "\n   Response filter: " + responseFilter.getClass().getName() +
	    "\n   Local host:       " + localHost + 
	    "\n   Local port:       " + localPort);

	if (proxy) {
		startMessage.append(
		"\n   Proxying requests");
	} else {
	    startMessage.append(
		"\n   Remote host:      " + remoteHost +
		"\n   Remote port:      " + remotePort);
	}

	if (rewriteURLs) {
	    startMessage.append(
		"\n   Rewriting absolute URLs for http://" + remoteHost);
	}

	if (useSSL) {
	    startMessage.append(
		"\n   Key store:        "  + keystore +
		"\n   Key password:     "  + keystorePassword +
		"\n (This could take a few seconds)");
	}

	System.err.println(startMessage);

	try {
	    if (proxy && useSSL) {

		// HTTPS proxy - depends on JSSE and regexp
		Class proxyEngineClass = null;
		
		try {
		    proxyEngineClass = Class.forName(HTTPS_PROXY_ENGINE_CLASS);
		}
		catch (ClassNotFoundException e) {
		    throw barfUsage(
			"Proxy engine '" + HTTPS_PROXY_ENGINE_CLASS +
			"' not found.\n" +
			"(You must install regexp and jsseto build it).");
		}

		final Class[] constructorSignature = {
		    SnifferFilter.class,
		    SnifferFilter.class,
		    String.class,
		    java.lang.Integer.TYPE,
		    java.lang.Integer.TYPE,
		    java.lang.Integer.TYPE,
		    java.lang.Boolean.TYPE,
		    String.class,
		    String.class };

		final Constructor constructor = 
		    proxyEngineClass.getConstructor(constructorSignature);

		final Object[] arguments = {
		    requestFilter,
		    responseFilter,
		    localHost,
		    new Integer(localPort),
		    new Integer(localSSLPort),
		    new Integer(timeout),
		    new Boolean(useColour),
		    keystore,
		    keystorePassword
		};

		m_snifferEngine =
		    (SnifferEngine)constructor.newInstance(arguments);


	    } else if (proxy && !useSSL) {
		// HTTP proxy - depends on regexp

		Class proxyEngineClass = null;
		
		try {
		    proxyEngineClass = Class.forName(HTTP_PROXY_ENGINE_CLASS);
		}
		catch (ClassNotFoundException e) {
		    throw barfUsage(
			"Proxy engine '" + HTTP_PROXY_ENGINE_CLASS +
			"' not found.\n" +
			"(You must install regexp to build it).");
		}

		final Class[] constructorSignature = {
		    SnifferFilter.class,
		    SnifferFilter.class,
		    String.class,
		    java.lang.Integer.TYPE,
		    java.lang.Integer.TYPE,
		    java.lang.Boolean.TYPE
		};

		final Constructor constructor = 
		    proxyEngineClass.getConstructor(constructorSignature);

		final Object[] arguments = {
		    requestFilter,
		    responseFilter,
		    localHost,
		    new Integer(localPort),
		    new Integer(timeout),
		    new Boolean(useColour)
		};

		m_snifferEngine =
		    (SnifferEngine)constructor.newInstance(arguments);

	    } else if (!proxy && useSSL) {
		// The SSL engine depends on JSSE. Load it dynamically
		// so we can build without it.

		Class sslEngineClass = null;
		
		try {
		    sslEngineClass = Class.forName(SSL_ENGINE_CLASS);
		}
		catch (ClassNotFoundException e) {
		    throw barfUsage("SSL engine '" + SSL_ENGINE_CLASS +
				    "' not found." +
				    "\n(You must install JSSE to build it).");
		}

		final Class[] constructorSignature = {
		    SnifferFilter.class,
		    SnifferFilter.class,
		    java.lang.Integer.TYPE,
		    String.class,
		    java.lang.Integer.TYPE,
		    java.lang.Boolean.TYPE,
		    String.class,
		    String.class };

		final Constructor constructor = 
		    sslEngineClass.getConstructor(constructorSignature);

		final Object[] arguments = {
		    requestFilter,
		    responseFilter,
		    localHost,
		    new Integer(localPort),
		    remoteHost,
		    new Integer(remotePort),
		    new Boolean(useColour),
		    keystore,
		    keystorePassword
		};

		m_snifferEngine =
		    (SnifferEngine)constructor.newInstance(arguments);
	    } else {
		m_snifferEngine = new SnifferEngineImpl(requestFilter,
							responseFilter,
							localHost,
							localPort,
							remoteHost,
							remotePort,
							useColour);
	    }
		
	    System.err.println("Engine initialised, listening on port " +
			       localPort);
	}
	catch (Exception e){
	    System.err.println("Could not initialise engine: ");
	    e.printStackTrace();
	}
    }

    private SnifferFilter instantiateFilter(String filterClassName)
    {
	if (filterClassName.equals("NONE")) {
	    return new NullFilter();
	}
	else if (filterClassName.equals("ECHO")) {
	    return new EchoFilter();
	}
	else if (filterClassName.equals("HTTP_PLUGIN")) {
	    return httpPluginFilterInstance();
	}

	final Class filterClass;
	
	try {
	    filterClass = Class.forName(filterClassName);
	}
	catch (ClassNotFoundException e){
	    throw barfUsage("Class '" + filterClassName + "' not found");
	}

	if (!SnifferFilter.class.isAssignableFrom(filterClass)) {
	    throw barfUsage("The specified filter class ('" +
			    filterClass.getName() +
			    "') does not implement the interface: '" +
			    SnifferFilter.class.getName() + "'");
	}

	// Instantiate a filter.
	try {
	    return (SnifferFilter)filterClass.newInstance();
	}
	catch (IllegalAccessException e) {
	    throw barfUsage("The default constructor of class '" +
			    filterClass.getName() + "' is not public");
	}
	catch (InstantiationException e) {
	    throw barfUsage("The class '" + filterClass.getName() +
			    "' does not have a default constructor");
	}
    }
	
    public void run() 
    {
	System.err.println("Starting engine");
	m_snifferEngine.run();
	System.err.println("Engine exited");
	System.exit(0);
    }

    /**
     * The HttpPluginSnifferFilter depends on Jakarta Regexp. Load it
     * *dynamically so we can build without it.
     */
    private SnifferFilter httpPluginFilterInstance() 
    {
	try {
	    final Class httpPluginFilter =
		Class.forName(HTTP_PLUGIN_FILTER_CLASS);

	    return (SnifferFilter)httpPluginFilter.newInstance();
	}
	catch (Exception e) {
	    throw barfUsage("HTTP Plugin Filter '" + HTTP_PLUGIN_FILTER_CLASS +
			    "' not found." +
			    "\n(You must have Jakarta Regexp to build it).");
	}
    }

    /**
     * The HttpsPluginSnifferFilter depends on Jakarta Regexp (but /not/ 
     * on JSSE!). Load it dynamically so we can build without it.
     */
    private SnifferFilter httpsPluginFilterInstance() 
    {
	try {
	    final Class httpsPluginFilter =
		Class.forName(HTTPS_PLUGIN_FILTER_CLASS);

	    return (SnifferFilter)httpsPluginFilter.newInstance();
	}
	catch (Exception e) {
	    throw barfUsage("HTTPS Plugin Filter '" 
			    + HTTPS_PLUGIN_FILTER_CLASS +
			    "' not found." +
			    "\n(You must have Jakarta Regexp to build it).");
	}
    }

    /**
     * The RewriteURLFilter depends on Jakarta Regexp. Load it
     * *dynamically so we can build without it.
     */
    private SnifferFilter urlRewriteFilterInstance() 
    {
	try {
	    final Class urlRewriteFilter =
		Class.forName(URL_REWRITE_FILTER_CLASS);

	    return (SnifferFilter)urlRewriteFilter.newInstance();
	}
	catch (Exception e) {
	    throw barfUsage("URL Rewrite Filter '" + URL_REWRITE_FILTER_CLASS +
			    "' not found." +
			    "\n(You must have Jakarta Regexp to build it).");
	}
    }

    /** Check we have the filter set up as Http filter */
    private boolean filterIsHttpFilter(SnifferFilter filter) {
	Class httpFilterClass = null;
	try {
	    httpFilterClass = Class.forName(HTTP_PLUGIN_FILTER_CLASS);

	    return httpFilterClass.isAssignableFrom(filter.getClass());
	} catch (Exception e) {
	    throw barfUsage("HTTP Plugin Filter '" + 
			    HTTP_PLUGIN_FILTER_CLASS + "' not found." +
			    "\n(You must have Jakarta Regexp to build it).");
	}
    }
}
