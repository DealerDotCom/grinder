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

package net.grinder.plugin.http;

import java.util.Set;


/**
 * Abstract interface to HTTP implementations such as HTTPClient and
 * HttpURLConnection.
 *
 * @author Philip Aston
 * @version $Revision$
 */
interface HTTPHandler
{
    public void reset() throws HTTPHandlerException;

    public String sendRequest(RequestData requestData)
	throws HTTPHandlerException;

    interface AuthorizationData 
    {
    }
    
    interface BasicAuthorizationData extends AuthorizationData
    {
	public String getRealm() throws HTTPHandlerException;
	public String getUser() throws HTTPHandlerException;
	public String getPassword() throws HTTPHandlerException;
    }

    interface RequestData
    {
	public Set getAdditionalHeaders() throws HTTPHandlerException;
	public AuthorizationData getAuthorizationData()
	    throws HTTPHandlerException;
	public String getContentType() throws HTTPHandlerException;
	public String getIfModifiedSince() throws HTTPHandlerException;
	public String getPostString() throws HTTPHandlerException;
	public String getURLString() throws HTTPHandlerException;
	public long getIfModifiedSinceLong() throws HTTPHandlerException;
    }
}
