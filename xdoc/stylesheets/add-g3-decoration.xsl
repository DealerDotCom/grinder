<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- This stylesheet adds G3 decoration to HTML -->
  <xsl:output method="xml" indent="no"/>

  <!-- Default identity transform -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="html/body">
    <body>
      <table>
	<td>
	  <p class="beta-warning">These pages are about
	    <em>The Grinder 3</em>. The Grinder 3 is currently in beta
	      release; if you want a stable version of The Grinder, look
	      to <a href="../manual/index.html"><em>The Grinder 2</em></a>
	      series. The information here is partially complete, and may
	      not reflect the final form of The Grinder 3.
	  </p>
	  <br/>
	  <xsl:copy-of select="."/>
	</td>
	<td valign="bottom" align="center">
	  <img align="center" src="../images/grinder3.jpg" alt="The Grinder 3" height="116" width="111"/>
	  <br/>
	  <img align="center" src="../images/PythonPoweredSmall.gif" alt="Python Powered" height="22" width="55"/>
	</td>
      </table>
    </body>
   </xsl:template>

</xsl:stylesheet>
 
