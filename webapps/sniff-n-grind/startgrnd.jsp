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