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

<%@ page import="java.io.*" %>

  <html>
	<head>
	  <title>Grinder results</title>
	</head>
	<body>
	  <h3>Grinder results</h3> 
	  <p>
		The results of your grinder session on <%=
		session.getAttribute("StartURL")%> are given below.
	  </p>
	  <p>
		<a href="startgrnd.jsp">Click here</a> to re-run this test
		with different grinder properties.<br>
		<a href="index.jsp">Click here</a> to record and run a new
		test.
	  </p>
	  <hr>

	  <% 

	String logdir = (String)session.getAttribute("ResultsDir")
	                + File.separator + "log";
	String[] files = (new File(logdir)).list();

	for(int i = 0; i < files.length; ++i) {
		String filename = logdir + File.separator + files[i];
		BufferedReader in = new BufferedReader(new FileReader(filename));
		out.println("<em>" + filename + "</em>:<br>");
		out.println("<pre>");
		String line = in.readLine();
		while (line != null) {
			out.println(line + " <br> ");
			line = in.readLine();
		}
		in.close();
		out.println("</pre>");
		out.println("<hr>");
	}

	  %>
	</body>
  </html>
