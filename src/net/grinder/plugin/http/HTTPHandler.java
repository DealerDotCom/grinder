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

package net.grinder.plugin.http;

import java.util.Map;


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
	public Map getHeaders() throws HTTPHandlerException;
	public AuthorizationData getAuthorizationData()
	    throws HTTPHandlerException;
	public String getPostString() throws HTTPHandlerException;
	public String getURLString() throws HTTPHandlerException;
    }
}
