// The Grinder
// Copyright (C) 2000  Paco Gomez

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

package net.grinder.plugin.http;

import net.grinder.plugininterface.PluginException;


/**
 * Interface to obtain the data needed to make a particular HTTP
 * request.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public interface HttpRequestData {
    public String getAuthorizationString() throws PluginException;
    public String getContextURLString();
    public String getPostString() throws PluginException;
    public String getURLString() throws PluginException;
    public long getIfModifiedSince();
}

