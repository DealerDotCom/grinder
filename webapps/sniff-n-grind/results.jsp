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
