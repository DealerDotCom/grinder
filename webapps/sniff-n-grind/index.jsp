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