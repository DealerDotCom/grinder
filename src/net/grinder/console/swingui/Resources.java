// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

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

package net.grinder.console.swingui;

import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;

import net.grinder.console.ConsoleException;


/**
 * Type safe interface to resource bundle.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class Resources
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
		return "?";
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

    private URL get(String key, boolean warnIfMissing)
    {
	final String name = getString(key, true);

	if (name.length() == 0) {
	    return null;
	}
	
	final URL url = this.getClass().getResource("resources/" + name);

	if (warnIfMissing && url == null) {
	    System.err.println("Warning - could not load resource " + name);
	}

	return url;
    }
}
