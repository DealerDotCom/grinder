<!--

 The Grinder
 Copyright (C) 2001  Paddy Spencer

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

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