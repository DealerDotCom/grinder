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
	  <title>Grinder</title>
	</head>
	<body>
	  <h3>Specify the grinder properties</h3>
	  Note that the following maxima are enforced:<br>
		5 processes; 25 threads; 50 cycles.<p>
	  <form method="post" action="grind">
		<input type="hidden" name="action" value="grindme">
		<table>
		  <tr>
			<td>
			  Processes: 
			</td>
			<td>
			  <input type="textfield" name="procs"><br>
			</td>
		  </tr>
		  <tr>
			<td>
			  Threads: 
			</td>
			<td>
			  <input type="textfield" name="threads"><br>
			</td>
		  </tr>
		  <tr>
			<td>
			  Cycles: 
			</td>
			<td>
			  <input type="textfield" name="cycles"><br>
			</td>
		  </tr>
		  <tr>
			<td>
			  <input type="reset"><br>
			</td>
			<td>
			  <input type="submit" value="Grind me, baby!"><br>
			</td>
		  </tr>
		</table>
	  </form>
  </html>
