<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- This stylesheet gets an xml conformant html page which has the
    additional <section> tag in the head -->
  <!-- Don't indent or pre sections get messed -->
  <xsl:output
    method="html"
    doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
    indent="no"
    doctype-system="http://www.w3.org/TR/html4/loose.dtd"/>

  <xsl:template match="html">
    <html>
      <head>
	<!-- copy the head to get title and meta data -->
	<xsl:copy-of select="head/node()"/>
	<title>The Grinder</title>
	<!-- change styles -->
	<style type="text/css">
	  <xsl:comment>
	    /* Sigh, IE doesn't inherit from body correctly; set children... */
	    p,ul,dl,ol,td,dd { color: rgb(0,0,0); font-family:Arial,Helvetica,sans-serif;  font-size: 10pt; }
	    pre { color: rgb(80,80,80); font-family:Courier, monospace; }
            code { color: rgb(80,80,80); font-family:Courier, monospace; }
	    dt { color: rgb(0,0,0); font-family:Arial,Helvetica,sans-serif; font-size: 11pt; font-weight: bold; }
	    a:link { color: rgb(0,0,128); text-decoration: none; font-weight: bold; }
	    a:visited { color: rgb(0,0,128); text-decoration: none; font-weight: bold; }
	    a:active { color: rgb(0,0,128); text-decoration: underline; font-weight: bold; }
	    a:hover { color: rgb(0,0,128); text-decoration: underline; font-weight: bold; }
	    td img { line-height:0; }
	    @media print
	    {
	    .layout { visibility: hidden; height: 0px; width: 0px; }
	    }
	    .beta-warning { background-color: rgb(224,224,255); font-weight: bold; padding: 3pt; width="50%"; margin-left: 10pt; }
	    .draft-warning { color: red; font-weight: bold; margin-top: 50pt; }
	    .sidebar { float: right; background-color: rgb(224,224,255); padding: 10pt; margin-left: 20pt; margin-right:20pt; margin-bottom:20pt; margin-top:20pt; font-size: 8pt; }
	    .table-heading { background-color: rgb(224,224,255); font-weight: bold; padding:2pt}

	    /* HTMLize */
	    .string { color: #008b00; background-color: #ffffff; } /* font-lock-string-face */
	    .type { color: #4682b4; background-color: #ffffff; } /* font-lock-type-face */
	    .function-name { color: #ff0000; background-color: #ffffff; } /* font-lock-function-name-face */
	    .keyword { color: #ff0000; background-color: #ffffff; } /* font-lock-keyword-face */
	    .comment { color: #00008b; background-color: #ffffff; font-family:Arial,Helvetica,sans-serif; } /* font-lock-comment-face */
	  </xsl:comment>
	</style>

	<!-- preload all needed xweb images -->
	<script language="JavaScript" type="text/javascript">
	  <xsl:comment>Begin
	    <!-- the section buttons -->
	    <xsl:for-each select="//img[contains(@xwebtype,'activeSection')]">
	      <xsl:value-of select="@name"/> = new Image( <xsl:value-of select="@width"/>,
		<xsl:value-of select="@height"/>);
		  <xsl:value-of select="@name"/>.src = "<xsl:value-of select="@src"/>";
	    </xsl:for-each>
	    <!-- the page buttons for the currently active section -->
	    <xsl:for-each select="//section[@active='true']">
	      <xsl:for-each select=".//img[@xwebtype='active']">
	      <xsl:value-of select="@name"/> = new Image( <xsl:value-of select="@width"/>,
	      <xsl:value-of select="@height"/>);
	      <xsl:value-of select="@name"/>.src = "<xsl:value-of select="@src"/>";
	    </xsl:for-each>
	  </xsl:for-each>
	  // End</xsl:comment>
	</script>
      </head>

      <body bgcolor="white">

	<!-- The table part contains a number of weird tricks to get
	 Netscape 4 to display this in a useful way. The algorithm for
	 calculating the width of columns in Netscape 4 is weird. Some
	 pages on the internet claim that they tried to reengineer it
	 but failed. Basic concept: values are minimum widths and the
	 rest is distributed across the columns. If the sum of the
	 widths is more than the screen width, a scrollbar appears
	 (and printouts are cut) -->

	<table cellspacing="0" cellpadding="0" border="0" width="100%">
	<!-- upper banner row: empty, defines the column widths -->
	  <tr bgcolor="#000080" class="layout">
	     <td width="5" bgcolor="#000080" style="width:5px;">&#160;</td>
	     <td width="90" bgcolor="#000080" style="width:90px;">&#160;</td>
	     <td width="15" bgcolor="#000080" style="width:15px;">&#160;</td>
	     <td width="85%" bgcolor="#000080" style="width:auto;">&#160;</td>
	     <td width="15%" bgcolor="#000080" style="width:0">&#160;</td>
	   </tr>
	   <!-- middle banner row: with logo part and banner -->
	   <tr bgcolor="#000080" style="background-color:rgb(00,00,128);" class="layout">
	     <td>&#160;</td>
	     <td align="center" valign="bottom">
	       <img src="{//file[@id='logo']/@src}" border="0" alt="The Grinder Logo" width="72" height="96"/>
	     </td>
	     <td>&#160;</td>
	     <td align="center" valign="middle">
	       <xsl:for-each select="//section[@active='true']">
		 <xsl:for-each select="img[@xwebtype='banner']">
		   <img src="{@src}" name="{@name}" border="0" alt="section banner" width="{@width}" height="{@height}"/>
		 </xsl:for-each>
	       </xsl:for-each>
	     </td>
	     <td>&#160;</td>
	   </tr>

	   <tr bgcolor="#000080" class="layout">
	     <td>&#160;</td>
	     <td>&#160;</td>
	     <td>&#160;</td>
	     <td>&#160;</td>
	     <td>&#160;</td>
	   </tr>

	   <!-- the main row -->
	   <tr valign="top">
	     <td class="layout">&#160;</td>
	     <!-- the navigation -->
	     <td align="center" class="layout">
	       <table cellspacing="0" cellpadding="0" border="0">
		 <xsl:apply-templates mode="nav" select="section"/>
	       </table>
	     </td>
	     <!-- spacing between nav and body -->
	     <td class="layout">&#160;</td>
	     <!-- the main body -->
	     <td>
	       <br/>
	       <xsl:copy-of select="body/node()"/>
	     </td>
	     <td>&#160;</td>
	   </tr>
	 </table>
       </body>
     </html>
   </xsl:template>

   <xsl:template match="section" mode="nav">
     <tr>
       <td align="center">
	 <a>
	   <xsl:attribute name="href"><xsl:value-of select="@src"/></xsl:attribute>
	   <xsl:choose>
	     <xsl:when test="@active='true' ">
	       <xsl:for-each select="img[@xwebtype='activeSection']">
		 <!-- should select exactly one -->
		 <img src="{@src}" name="{@name}" border="0" alt="{concat(@alt, ' (active section)')}" width="{@width}" height="{@height}"/>
	       </xsl:for-each>
	     </xsl:when>
	     <xsl:otherwise>
	       <xsl:attribute name="onMouseOver"><xsl:text>document.</xsl:text><xsl:value-of select="img[@xwebtype='normalSection']/@name"/><xsl:text>.src='</xsl:text><xsl:value-of select="img[@xwebtype='activeSection']/@src"/><xsl:text>';</xsl:text></xsl:attribute>
	       <xsl:attribute name="onMouseOut"><xsl:text>document.</xsl:text><xsl:value-of select="img[@xwebtype='normalSection']/@name"/><xsl:text>.src='</xsl:text><xsl:value-of select="img[@xwebtype='normalSection']/@src"/><xsl:text>';</xsl:text></xsl:attribute>
	       <xsl:for-each select="img[@xwebtype='normalSection']">
		 <!-- should select exactly one -->
		 <img src="{@src}" name="{@name}" border="0" alt="{concat(@alt, ' (section)')}" width="{@width}" height="{@height}"/>
	       </xsl:for-each>
	     </xsl:otherwise>
	   </xsl:choose>
	 </a>
       </td>
     </tr>
     <xsl:if test="@active='true' ">
       <xsl:apply-templates mode="nav" select="entry"/>
     </xsl:if>
   </xsl:template>

   <xsl:template match="entry" mode="nav">
     <!-- we ignore the first entry (it is addressed by the section button) -->
     <xsl:if test="position() != 1">
       <tr>
	 <td align="center">
	   <a>
	     <xsl:attribute name="href"><xsl:value-of select="@src"/></xsl:attribute>
	     <xsl:choose>
	       <xsl:when test="@active">
		 <xsl:for-each select="img[@xwebtype='active']">
		 <!-- should select exactly one -->
		   <img src="{@src}" name="{@name}" border="0" alt="{concat(@alt, ' (active)')}" width="{@width}" height="{@height}"/>
		 </xsl:for-each>
	       </xsl:when>
	       <xsl:otherwise>
		 <xsl:attribute name="onMouseOver"><xsl:text>document.</xsl:text><xsl:value-of select="img[@xwebtype='normal']/@name"/><xsl:text>.src='</xsl:text><xsl:value-of select="img[@xwebtype='active']/@src"/><xsl:text>';</xsl:text></xsl:attribute>
		 <xsl:attribute name="onMouseOut"><xsl:text>document.</xsl:text><xsl:value-of select="img[@xwebtype='normal']/@name"/><xsl:text>.src='</xsl:text><xsl:value-of select="img[@xwebtype='normal']/@src"/><xsl:text>';</xsl:text></xsl:attribute>
		 <xsl:for-each select="img[@xwebtype='normal']">
		 <!-- should select exactly one -->
		   <img src="{@src}" name="{@name}" border="0" alt="{@alt}" width="{@width}" height="{@height}"/>
		 </xsl:for-each>
	       </xsl:otherwise>
	     </xsl:choose>
	   </a>
	 </td>
       </tr>
       <!-- put an empty row below the last entry of a section to get some spacing -->
       <xsl:if test="position() = last()">
	 <tr>
	   <td>
	     &#160;
	   </td>
	 </tr>
       </xsl:if>
     </xsl:if>
  </xsl:template>
</xsl:stylesheet>
 
