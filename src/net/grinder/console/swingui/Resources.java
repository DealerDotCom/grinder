// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
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

package net.grinder.console.swingui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.DisplayMessageConsoleException;


/**
 * Type safe interface to resource bundle.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class Resources implements net.grinder.console.common.Resources
{
    private static ResourceBundle s_resources = null;

    public Resources()
	throws ConsoleException
    {
	synchronized (Resources.class) {
	    if (s_resources == null) {
		try { 
		    s_resources = ResourceBundle.getBundle(
			"net.grinder.console.swingui.resources.Console");
		}
		catch (MissingResourceException e) {
		    throw new ConsoleException("Resource bundle not found");
		}
	    }

	    DisplayMessageConsoleException.setResources(this);
	}
    }

    /**
     * Overloaded version of {@link #getString(String, boolean)} which writes out a
     * waning if the resource is missing.
     * @param key The resource key.
     * @return The string.
     **/
    public String getString(String key)
    {
	return getString(key, true);
    }

    /**
     * Use key to look up resource which names image URL. Return the image.
     * @param key The resource key.
     * @param warnIfMissing true => write out an error message if the resource is missing.
     * @return The string.
     **/
    public String getString(String key, boolean warnIfMissing)
    {
	try {
	    return s_resources.getString(key);
	}
	catch (MissingResourceException e) {
	    if (warnIfMissing) {
		System.err.println(
		    "Warning - resource " + key + " not specified");
		return "";
	    }

	    return null;
	}
    }

    /**
     * Overloaded version of {@link #getImageIcon(String, boolean)} which doesn't write out a
     * waning if the resource is missing.
     * @param key The resource key.
     * @return The image.
     **/
    public ImageIcon getImageIcon(String key)
    {
	return getImageIcon(key, false);
    }

    /**
     * Use key to look up resource which names image URL. Return the image.
     * @param key The resource key.
     * @param warnIfMissing true => write out an error message if the resource is missing.
     * @return The image
     **/
    public ImageIcon getImageIcon(String key, boolean warnIfMissing)
    {
	final URL resource = get(key, warnIfMissing);

	return resource != null ? new ImageIcon(resource) : null;
    }

    public String getStringFromFile(String key, boolean warnIfMissing)
    {
	final URL resource = get(key, warnIfMissing);

	if (resource != null) {
	    try {
		final Reader in =
		    new BufferedReader(
			new InputStreamReader(resource.openStream()));

		final StringWriter out = new StringWriter();

		int c;

		while ((c = in.read()) > 0) {
		    out.write(c);
		}

		in.close();
		out.close();

		return out.toString();
	    }
	    catch (IOException e) {
		System.err.println("Warning - could not reading " + resource);
	    }
	}

	return null;
    }

    private URL get(String key, boolean warnIfMissing)
    {
	final String name = getString(key, warnIfMissing);

	if (name == null || name.length() == 0) {
	    return null;
	}
	
	final URL url = this.getClass().getResource("resources/" + name);

	if (url == null) {
	    System.err.println("Warning - could not load resource " + name);
	}

	return url;
    }
}
