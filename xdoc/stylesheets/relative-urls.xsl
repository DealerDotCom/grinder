<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- This stylesheet adds a table of contents to an xhtml document -->
  <xsl:output method="xml" indent="yes"/>

  <!-- Declare all the parameters we understand -->
  <xsl:param name="base" select="'** NOT SET **'"/>

  <!-- Default identity transform -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="/html/section|/html/section//*[@src]|directory//*[@src]">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:attribute name="src">
	<xsl:value-of select="$base"/>
	<xsl:text>/</xsl:text>
	<xsl:value-of select="substring-after(@src, 'BASE/')"/>
      </xsl:attribute>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
