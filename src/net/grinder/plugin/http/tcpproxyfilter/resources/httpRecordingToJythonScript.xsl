<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:g="http://grinder.sourceforge.net/tcpproxy/http/1.0"
  xmlns:helper="net.grinder.plugin.http.tcpproxyfilter.XSLTHelper">

  <xsl:output method="text"/>
  <xsl:strip-space elements="*"/>


  <xsl:template match="g:http-recording">
    <xsl:value-of select="concat('# ', g:metadata/g:version)"/>

    <xsl:text>
# HTTP script recorded by TCPProxy at </xsl:text>
    <xsl:value-of select="helper:formatTime(g:metadata/g:time)"/>

    <xsl:text>

from net.grinder.script import Test
from net.grinder.script.Grinder import grinder
from net.grinder.plugin.http import HTTPPluginControl, HTTPRequest
from HTTPClient import NVPair
connectionDefaults = HTTPPluginControl.getConnectionDefaults()
httpUtilities = HTTPPluginControl.getHTTPUtilities()
</xsl:text>

    <xsl:apply-templates select="g:base-url"/>
    <xsl:value-of select="helper:newLine()"/>
    <xsl:apply-templates select="g:common-headers"/>
    <xsl:apply-templates select="g:request"/>

    <xsl:text>
# An TestRunner instance is created for each worker thread
class TestRunner:
</xsl:text>

    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text># This method is called for every run</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>def __call__(self):</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLine()"/>

    <xsl:apply-templates select="g:request" mode="__call__"/>

  </xsl:template>


  <xsl:template match="g:base-url">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="concat(@url-id, ' = ')"/>
    <xsl:text>'</xsl:text>
    <xsl:value-of select="concat(g:scheme, '://', g:host, ':', g:port)"/>
    <xsl:text>'</xsl:text>
  </xsl:template>


  <xsl:template match="g:common-headers[@headers-id='defaultHeaders']">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:text>connectionDefaults.defaultHeaders = \</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:call-template name="tuple-list"/>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:common-headers">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="concat(@headers-id, '= \')"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:call-template name="tuple-list"/>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:request">
    <xsl:variable name="request-name" select="concat('request', @request-id)"/>

    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="$request-name"/>
    <xsl:text> = HTTPRequest(url=</xsl:text>
    <xsl:value-of select="g:url/@extends"/>
    <xsl:text>, headers=</xsl:text>
    <xsl:value-of select="g:headers/@extends"/>
    <xsl:text>)</xsl:text>

    <xsl:if test="g:body/g:file">
      <xsl:value-of select="helper:newLine()"/>
      <xsl:value-of select="$request-name"/>
      <xsl:text>.setDataFromFile('</xsl:text>
      <xsl:value-of select="g:body/g:file"/>
      <xsl:text>')</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="concat('test', @request-id)"/>
    <xsl:text> = Test(</xsl:text>
    <xsl:value-of select="@request-id"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="helper:quoteForPython(g:short-description)"/>
    <xsl:text>).wrap(</xsl:text>
    <xsl:value-of select="$request-name"/>
    <xsl:text>)</xsl:text>

    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:request" mode="__call__">
    <xsl:apply-templates select="g:sleep-time" mode="__call__"/>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="concat('test', @request-id, '.')"/>
    <xsl:value-of select="g:method"/>
    <xsl:text>('</xsl:text>
    <xsl:value-of select="g:url/g:path"/>
    <xsl:text>'</xsl:text>

    <xsl:apply-templates select="g:url/g:query-string" mode="__call__"/>

    <xsl:apply-templates select="g:body" mode="__call__"/>

    <xsl:apply-templates select="g:headers" mode="__call__"/>

    <xsl:text>)</xsl:text>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:sleep-time" mode="__call__">
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="concat('grinder.sleep(', ., ')')"/>
  </xsl:template>


  <xsl:template match="g:query-string/g:parsed" mode="__call__">
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:call-template name="tuple-list"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>


  <xsl:template match="g:query-string/g:unparsed" mode="__call__">
    <xsl:text> +</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="helper:quoteForPython(concat('?', .))"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>


  <xsl:template match="g:body/g:binary" mode="__call__">
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="helper:base64ToPython(.)"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>


  <xsl:template match="g:body/g:file" mode="__call__">

    <!-- Data file is read at top level. We provide a parameter here
    to disambiguate the POST call if per-request headers are
    specified.-->
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="concat('request', ../../@request-id, '.data')"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>

 </xsl:template>


  <xsl:template match="g:body/g:form" mode="__call__">
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:call-template name="tuple-list"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>


  <xsl:template match="g:body/g:string" mode="__call__">
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="helper:quoteForPython(.)"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>


  <xsl:template match="g:headers[node()]" mode="__call__">
    <xsl:if test="not(../g:url/g:query-string/g:parsed|../g:body)">
      <!-- No keyword arguments for methods, insert dummy parameter. -->
      <xsl:text>, ()</xsl:text>
    </xsl:if>

    <xsl:text>,</xsl:text>

    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:call-template name="tuple-list"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>

  <xsl:template match="g:header|g:parameter|g:form-field" mode="tuple">
    <xsl:call-template name="indent-tuple-entry"/>

    <xsl:text>NVPair(</xsl:text>
    <xsl:value-of select="helper:quoteForPython(@name)"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="helper:quoteForPython(@value)"/>
    <xsl:text>),</xsl:text>
  </xsl:template>

  <xsl:template match="g:authorization/g:basic" mode="tuple">
    <xsl:call-template name="indent-tuple-entry">
      <xsl:with-param name="first-entry" select="not(../preceding-sibling::*)"/>
    </xsl:call-template>

    <xsl:text>httpUtilities.basicAuthorization(</xsl:text>
    <xsl:value-of select="helper:quoteForPython(@userid)"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="helper:quoteForPython(@password)"/>
    <xsl:text>),</xsl:text>
  </xsl:template>

  <xsl:template name="tuple-list" mode="__call__">
    <xsl:text>(</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>

    <xsl:apply-templates mode="tuple"/>

    <xsl:text> )</xsl:text>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>

  <xsl:template name="indent-tuple-entry">
    <xsl:param name="first-entry" select="not(preceding-sibling::*)"/>

    <xsl:choose>
      <xsl:when test="$first-entry"><xsl:text> </xsl:text></xsl:when>
      <xsl:otherwise><xsl:value-of select="helper:newLineAndIndent()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Ignore these nodes -->
  <xsl:template match="g:body/g:content-type" mode="__call__"/>

</xsl:stylesheet>
