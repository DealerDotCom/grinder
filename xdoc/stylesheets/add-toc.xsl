<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- This stylesheet adds a table of contents to an xhtml document -->
  <xsl:output method="xml" indent="yes"/>

  <!-- Default identity transform -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <!-- Currently only support one TOC per page. -->
  <xsl:template match="toc">
    <ol>
      <xsl:for-each select="//a[@name]">
	<li>
	  <xsl:copy>
	    <xsl:attribute name="href">
	      <xsl:text>#</xsl:text>
	      <xsl:value-of select="@name"/>
	    </xsl:attribute>
	    <xsl:value-of select="."/>
	  </xsl:copy>
	</li>
      </xsl:for-each>
    </ol>
  </xsl:template>

  <xsl:template match="//a[@name]">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:attribute name="href">
	<xsl:text>#</xsl:text>
      </xsl:attribute>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
