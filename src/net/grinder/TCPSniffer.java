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

import net.grinder.plugin.http.HttpPluginSnifferFilter;
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
	    "\n   [-httpPluginFilter]            See below" +
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
	    "\n   '-requestFilter HTTP_PLUGIN -responseFilter NONE'" +
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
		    requestFilter = new HttpPluginSnifferFilter();
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
		else {
		    throw barfUsage();
		}

		++i;
	    }
	}
	catch (Exception e) {
	    throw barfUsage();
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
	    "\n   Local port:       " + localPort +
	    "\n   Remote host:      " + remoteHost +
	    "\n   Remote port:      " + remotePort
	    );

	if (useSSL) {
	    startMessage.append(
		"\n   Key store:        "  + keystore +
		"\n   Key password:     "  + keystorePassword +
		"\n (This could take a few seconds)");
	}

	System.err.println(startMessage);

	try {
	    if (!useSSL) {
		m_snifferEngine =
		    new SnifferEngineImpl(requestFilter,
					  responseFilter,
					  localPort,
					  remoteHost,
					  remotePort);
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
	    return new HttpPluginSnifferFilter();
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
}



