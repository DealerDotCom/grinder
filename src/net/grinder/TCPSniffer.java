// The Grinder
// Copyright (C) 2000  Paco Gomez
// Copyright (C) 2000  Phil Dawes
// Copyright (C) 2001  Phil Aston

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
	    "\n   ]" +
	    "\n   [-localPort <port>]            Default is 8001" +
	    "\n   [-remoteHost <host name>]      Default is localhost" +
	    "\n   [-remotePort <port>]           Default is 7001" +
	    "\n" +
	    "\n   [-ssl                          Use SSL" +
	    "\n    [-certificate <PKCS12 file>   Optional client certificate" +
	    "\n     -password <password>]        Certificate keystore pass" +
	    "\n   ]" +
	    "\n" +
	    "\n <filter> can be the name of a class that implements " +
	    "\n " + SnifferFilter.class.getName() + " or " +
	    "\n one of NONE, ECHO, or HTTP_PLUGIN. Default is ECHO." +
	    "\n" +
	    "\n -httpPluginFilter is a synonym for" +
	    "\n '-requestFilter HTTP_PLUGIN -responseFilter NONE'" +
	    "\n" +
	    "\n -rewriteURLs will cause absolute URLs to remoteHost in" +
	    "\n pages from remoteHost to be rewritten as relative URLs." +
	    "\n" +
	    "\n -proxy means the sniffer will act as an http proxy" +
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
    private final String PROXY_ENGINE_CLASS =
	"net.grinder.plugin.http.HttpProxySnifferEngineImpl";
    private final String HTTP_PLUGIN_FILTER_CLASS =
	"net.grinder.plugin.http.HttpPluginSnifferFilter";
    private final String URL_REWRITE_FILTER_CLASS =
	"net.grinder.plugin.http.URLRewriteFilter";

    private TCPSniffer(String args[])
    {
	// Default values.
	SnifferFilter requestFilter = new EchoFilter();
	SnifferFilter responseFilter = new EchoFilter();
	int localPort = 8001;
	String remoteHost = "localhost";
	int remotePort = 7001;
	boolean useSSL = false;
	String keystore = null;
	String keystorePassword = null;
	boolean rewriteURLs = false;
	boolean proxy = false;

	int i = 0;

	try {
	    while (i < args.length)
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
		else if (args[i].equals("-rewriteURLs")) {
		    rewriteURLs = true;
		}
		else if (args[i].equals("-proxy")) {
		    proxy = true;
		}
		else {
		    throw barfUsage();
		}

		++i;
	    }
	}
	catch (Exception e) {
	    throw barfUsage();
	}

	if (proxy) {
	    // we need a better way of detecting if these have been set
	    if( !remoteHost.equals("localhost") || remotePort != 7001) {
		throw barfUsage("Don't set remoteHost or remotePort while "
				+ "using the sniffer in proxy mode");
	    }

	    if (!filterIsHttpFilter(requestFilter)) {
		throw barfUsage("Specify HTTP_PLUGIN as the request filter " +
				"when using -proxy");
	    }
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
	    "\n   Local port:       " + localPort);

	if (!proxy) {
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

	    // currently don't do an SSL proxy
	    if (!useSSL && !proxy) {
		m_snifferEngine =
		    new SnifferEngineImpl(requestFilter,
					  responseFilter,
					  localPort,
					  remoteHost,
					  remotePort);
	    }
	    else if (!useSSL) {
		// proxy engine uses regexp so load it dynamically
		Class proxyEngineClass = null;
		
		try {
		    proxyEngineClass = Class.forName(PROXY_ENGINE_CLASS);
		}
		catch (ClassNotFoundException e) {
		    throw barfUsage("Proxy engine '" + PROXY_ENGINE_CLASS +
				    "' not found." +
				    "\n(You must install regexp to build it).");
		}

		final Class[] constructorSignature = {
		    SnifferFilter.class,
		    SnifferFilter.class,
		    java.lang.Integer.TYPE };

		final Constructor constructor = 
		    proxyEngineClass.getConstructor(constructorSignature);

		final Object[] arguments = {
		    requestFilter,
		    responseFilter,
		    new Integer(localPort),
		};

		m_snifferEngine =
		    (SnifferEngine)constructor.newInstance(arguments);
	    }
	    else {
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
		    String.class,
		    String.class };

		final Constructor constructor = 
		    sslEngineClass.getConstructor(constructorSignature);

		final Object[] arguments = {
		    requestFilter,
		    responseFilter,
		    new Integer(localPort),
		    remoteHost,
		    new Integer(remotePort),
		    keystore,
		    keystorePassword
		};

		m_snifferEngine =
		    (SnifferEngine)constructor.newInstance(arguments);
	    }

	    System.err.println("Engine initialised, listening on port " +
			       localPort);
	}
	catch (Exception e){
	    System.err.println("Could not initialise engine: ");
	    e.printStackTrace(System.err);
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
