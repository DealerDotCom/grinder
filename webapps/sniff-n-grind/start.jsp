<!--

Copyright (C) 2001 Paddy Spencer
All rights reserved.

This file is part of The Grinder software distribution. Refer to
the file LICENSE which is part of The Grinder distribution for
licensing details. The Grinder distribution is available on the
Internet at http://grinder.sourceforge.net/

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
OF THE POSSIBILITY OF SUCH DAMAGE.

-->

  <html>
	<head>
	  <title>Grinder Go!</title>
	</head>
	<body>
	  <table border="0" width="100%">
		
		<tr>
		  <td colspan="2" align="left">
			<font size="+2"<em>The sniffer has been started</em></font>
		  </td>
		</tr>
		<tr><td colspan="2">&nbsp;</td></tr>
		<tr>
			<td width="50%" align="right" valign="top">
			<font size="+1">
			  Edit your browser 
			  <%
				Boolean secure = (Boolean)session.getAttribute("IsSecure");
				if (secure.booleanValue()) {
					out.println("https/ssl");
				} else {
					out.println("http");
				}
			  %>
			  proxy settings -&nbsp;
			</font>
			</td>
			<td width="50%" align="left" valign="top">
			<font size="+1">
			  host &nbsp;:&nbsp; <b><code>
				<%
				String host = java.net.InetAddress.getLocalHost().toString();
				out.println(host.substring(0, host.indexOf("/")));
				%>
				</code></b>
				<br> 
				port &nbsp;:&nbsp; 
				<b><code><%= session.getAttribute("port") %></code></b>
			</font>
			</td>
		</tr>
		<tr><td colspan="2">&nbsp;</td></tr>
		<tr>
		  <td colspan="2" align="center">
			<center>
			  <!-- I was going to use a form here to go to the
			  starting URL, but that ended up with the URL as a query
			  string, ending in ? and I couldn't be bothered to write
			  some servlet code to parse it when a simple href link
			  works fine -->
			  <a href='<%= session.getAttribute("StartURL")%>'
				 target="none">Click here to go to 
				<%=	session.getAttribute("StartURL") %> 
				and begin the test</a>
			</center>
		  </td>
		</tr>
		<tr><td colspan="2">&nbsp;</td></tr>
		<tr>
		  <td colspan="2" align="center">
			<form action="sniff" method="post"
				  target="_top"> 
				<input type="hidden" name="action" value="stop">
				When you have finished, <font color="red">reset your
				  proxy</font> and then click on the button below<p>
				<input type="submit" value="click here to stop the test">
			</form>
		  </td>
		</tr>
	  </table>
	  
	</body>
  </html>
