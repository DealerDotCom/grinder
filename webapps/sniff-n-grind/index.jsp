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
	  <title>Grinder Setup</title>
	</head>
	<body>
	  <h3>Grinder Sniffer Setup</h3>
	  
	  <form action="sniff" method="post">
		<table>
		  <tr>
			<td>
			  <input type="hidden" name="action" value="start">
				Please enter your starting URL:<br>
				(include either <code>http://</code> or
				<code>https://</code>)				
			</td>
			<td>
			  <input type="textfield" name="StartURL"
					 value="http://localhost:7001/"><br>
			</td>
		  </tr>
		  <tr>
			<td>
			</td> 
			<td>
			  <input type="submit" value="Get sniffing!">
			</td>
		  </tr>
		</table>
	  </form>

	</body>
  </html>
