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
	  <title>Sniffer results</title>
	</head>
	<body>
	  <h3>Sniffer results</h3> 
	  <p>
		The results of <code>"httpsniffer.out"</code> need to be
		pasted into your <code>grinder.properties</code>. Each of the
		<code>post</code> sections (if any) needs to be saved into a
		file with the correct name, in the same directory as your
		<code>grinder.properties</code> file.
	  </p>
	  <p>
		<a href="startgrnd.jsp">Click here</a> if you want to run the
		grinder automatically.
	  </p>
	  <hr>

	  <% 

	String workdir = (String)session.getAttribute("ResultsDir");
	String[] files = (new File(workdir)).list();

	for(int i = 0; i < files.length; ++i) {
		String filename = workdir + File.separator + files[i];
		BufferedReader in = new BufferedReader(new FileReader(filename));
		out.println("<em>" + filename + "</em>:<br>");
		String line = in.readLine();
		// the indexOf is a blatant hack - a better way exists but
		// this will do
		while (line != null && line.indexOf("sniff?action=stop") == -1) {
			out.println(line + " <br> ");
			line = in.readLine();
		}
		in.close();
		out.println("<hr>");
	}

	  %>

	</body>
  </html>
