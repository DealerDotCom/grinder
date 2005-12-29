<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:g="http://grinder.sourceforge.net/tcpproxy/http/1.0"
  xmlns:helper="net.grinder.plugin.http.tcpproxyfilter.XSLTHelper">

  <xsl:output method="text"/>
  <xsl:strip-space elements="*"/>


  <xsl:template match="g:http-recording">
    <xsl:text># </xsl:text>
    <xsl:value-of select="g:metadata/g:version"/>

    <xsl:text>
# HTTP script recorded by TCPProxy at </xsl:text>
    <xsl:value-of select="helper:formatTime(g:metadata/g:time)"/>

    <xsl:text>

from net.grinder.script import Test
from net.grinder.script.Grinder import grinder
from net.grinder.plugin.http import HTTPRequest
from HTTPClient import NVPair
</xsl:text>

    <xsl:apply-templates select="g:base-url"/>
    <xsl:value-of select="helper:newLine()"/>
    <xsl:apply-templates select="g:common-headers"/>
    <xsl:apply-templates select="g:request"/>

    <xsl:text>
# An TestRunner instance is created for each worker thread
class TestRunner:
</xsl:text>

    <xsl:value-of select="helper:newLineAndIndent(1)"/>
    <xsl:text># This method is invoked for every run</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent(1)"/>
    <xsl:text>def __call__(self):</xsl:text>
    <xsl:value-of select="helper:newLine()"/>

    <xsl:apply-templates select="g:request" mode="thread"/>

  </xsl:template>

  <xsl:template match="g:base-url">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="@url-id"/>
    <xsl:text> = '</xsl:text>
    <xsl:value-of select="g:scheme"/>
    <xsl:text>://</xsl:text>
    <xsl:value-of select="g:host"/>
    <xsl:text>:</xsl:text>
    <xsl:value-of select="g:port"/>
    <xsl:text>'</xsl:text>
  </xsl:template>

  <xsl:template match="g:common-headers">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="@headers-id"/>
    <xsl:text> = \</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent(1)"/>
    <xsl:value-of select="helper:formatNVPairList(g:header, 1)"/>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:request">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:text>request</xsl:text>
    <xsl:value-of select="@request-id"/>
    <xsl:text> = HTTPRequest(url=</xsl:text>
    <xsl:value-of select="g:url/@extends"/>
    <xsl:text>, headers=</xsl:text>
    <xsl:value-of select="g:headers/@extends"/>
    <xsl:text>)</xsl:text>

    <xsl:value-of select="helper:newLine()"/>
    <xsl:text>test</xsl:text>
    <xsl:value-of select="@request-id"/>
    <xsl:text> = Test(</xsl:text>
    <xsl:value-of select="@request-id"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="helper:quoteForPython(g:short-description)"/>
    <xsl:text>).wrap(</xsl:text>
    <xsl:text>request</xsl:text>
    <xsl:value-of select="@request-id"/>
    <xsl:text>)</xsl:text>

    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>

  <xsl:template match="g:request" mode="thread">
    <xsl:apply-templates select="g:sleep-time"/>

    <xsl:value-of select="helper:newLineAndIndent(2)"/>
    <xsl:text>test</xsl:text>
    <xsl:value-of select="@request-id"/>
    <xsl:text>.</xsl:text>
    <xsl:value-of select="g:method"/>
    <xsl:text>('</xsl:text>
    <xsl:value-of select="g:url/g:path"/>
    <xsl:text>'</xsl:text>

    <xsl:apply-templates select="g:url/g:query-string"/>

    <xsl:apply-templates select="g:body"/>

    <xsl:apply-templates select="g:headers"/>

    <xsl:text>)</xsl:text>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:sleep-time">
    <xsl:value-of select="helper:newLineAndIndent(2)"/>
    <xsl:text>grinder.sleep(</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>)</xsl:text>
  </xsl:template>

  <xsl:template match="g:query-string/g:parsed">
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent(3)"/>
    <xsl:value-of select="helper:formatNVPairList(g:parameter, 3)"/>
  </xsl:template>

  <xsl:template match="g:query-string/g:unparsed">
    <xsl:text> +</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent(3)"/>
    <xsl:value-of select="helper:quoteForPython(concat('?', .))"/>
  </xsl:template>


  <xsl:template match="g:body/g:form">
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent(3)"/>
    <xsl:value-of select="helper:formatNVPairList(g:form-field, 3)"/>
  </xsl:template>

  <xsl:template match="g:body/g:binary">
    TODO, and what about the headers param?
    TODO ALSO: STATEFUL INDENTATION
  </xsl:template>

  <xsl:template match="g:headers[g:header]">
    <xsl:if test="not(../g:url/g:query-string/g:parsed|../g:body)">
      <!-- Can't use keyword arguments for methods, insert dummy
      parameter. -->
      <xsl:text>,</xsl:text>
      <xsl:value-of select="helper:newLineAndIndent(3)"/>
      <xsl:text>()</xsl:text>
    </xsl:if>

    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent(3)"/>
    <xsl:value-of select="helper:formatNVPairList(g:header, 3)"/>
  </xsl:template>

  <!-- Traverse down these nodes -->
  <xsl:template match="g:query-string|g:body">
    <xsl:apply-templates select="@*"/>
    <xsl:apply-templates/>
  </xsl:template>

  <!-- Ignore these nodes -->
  <xsl:template match="g:content-type"/>

  <!-- Default identity transform -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
