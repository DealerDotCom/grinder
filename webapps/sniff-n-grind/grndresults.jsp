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
