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

# These definitions at the top level of the file are evaluated once,
# when the worker process is started.
</xsl:text>

    <xsl:apply-templates select="*" mode="file"/>

    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="helper:newLine()"/>
    <xsl:text>class TestRunner:</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"""An TestRunner instance is created for each worker thread."""</xsl:text>
    <xsl:value-of select="helper:newLine()"/>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>def __call__(self):</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"""This method is called for every run perfomed by the worker thread."""</xsl:text>
    <xsl:value-of select="helper:newLine()"/>

    <xsl:apply-templates select="*" mode="__call__"/>
    <xsl:value-of select="helper:newLine()"/>

    <xsl:value-of select="helper:changeIndent(-1)"/>

    <xsl:apply-templates select="*" mode="TestRunner"/>

  </xsl:template>


  <xsl:template match="g:base-url" mode="file">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="concat(@url-id, ' = ')"/>
    <xsl:text>'</xsl:text>
    <xsl:value-of select="concat(g:scheme, '://', g:host, ':', g:port)"/>
    <xsl:text>'</xsl:text>

    <xsl:if test="not(following::g:base-url)">
      <xsl:value-of select="helper:newLine()"/>
    </xsl:if>
  </xsl:template>


  <xsl:template match="g:common-headers[@headers-id='defaultHeaders']" mode="file">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:text>connectionDefaults.defaultHeaders = \</xsl:text>
    <xsl:call-template name="tuple-list"/>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:common-headers" mode="file">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="concat(@headers-id, '= \')"/>
    <xsl:call-template name="tuple-list"/>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>

  <xsl:template match="g:request" mode="file">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:variable name="request-name" select="concat('request', @request-id)"/>
    <xsl:value-of select="$request-name"/>

    <xsl:value-of select="concat(' = HTTPRequest(url=', g:url/@extends)"/>
    <xsl:value-of select="concat(', headers=', g:headers/@extends, ')')"/>

    <xsl:if test="g:body/g:file">
      <xsl:value-of select="helper:newLine()"/>
      <xsl:value-of select="$request-name"/>
      <xsl:text>.setDataFromFile('</xsl:text>
      <xsl:value-of select="g:body/g:file"/>
      <xsl:text>')</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="concat('test', @request-id, '= Test(')"/>
    <xsl:value-of select="concat(@request-id, ', ')"/>
    <xsl:value-of select="helper:quoteForPython(g:description)"/>
    <xsl:value-of select="concat(').wrap(', $request-name, ')')"/>

    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:page" mode="file">
    <xsl:apply-templates select="*" mode="file"/>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="concat('def ', @page-id, '():')"/>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"""Execute requests for </xsl:text>
    <xsl:apply-templates select="*" mode="pageDescription"/>
    <xsl:text>."""</xsl:text>
    <xsl:apply-templates select="*" mode="page"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>


  <xsl:template match="g:page" mode="__call__">
    <xsl:apply-templates select="*" mode="__call__"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="concat(@page-id, '()')"/>
    <xsl:call-template name="indent">
      <xsl:with-param name="characters" select="10-string-length(@page-id)"/>
    </xsl:call-template>
    <xsl:text> # </xsl:text>
    <xsl:apply-templates select="*" mode="pageDescription"/>
  </xsl:template>


  <xsl:template match="g:request[position() = 1]" mode="pageDescription">
    <xsl:value-of select="g:description"/>
  </xsl:template>


  <xsl:template match="g:request" mode="page">
    <xsl:apply-templates select="g:sleep-time" mode="request"/>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="concat('test', @request-id, '.')"/>
    <xsl:value-of select="g:method"/>
    <xsl:text>('</xsl:text>
    <xsl:value-of select="g:url/g:path"/>
    <xsl:text>'</xsl:text>

    <xsl:apply-templates select="g:url/g:query-string" mode="request"/>

    <xsl:apply-templates select="g:body" mode="request"/>

    <xsl:apply-templates select="g:headers" mode="request"/>

    <xsl:text>)</xsl:text>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:sleep-time[../preceding-sibling::g:request]" mode="request">
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="concat('grinder.sleep(', ., ')')"/>
  </xsl:template>

  <!--  First sleep() for a page appears in the __call__ block. -->
  <xsl:template match="g:sleep-time[not(../preceding-sibling::g:request)]" mode="__call__">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="concat('grinder.sleep(', ., ')')"/>
  </xsl:template>

  <xsl:template match="g:query-string/g:parsed" mode="request">
    <xsl:text>,</xsl:text>
    <xsl:call-template name="tuple-list"/>
  </xsl:template>


  <xsl:template match="g:query-string/g:unparsed" mode="request">
    <xsl:text> +</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="helper:quoteForPython(concat('?', .))"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>


  <xsl:template match="g:body/g:binary" mode="request">
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="helper:base64ToPython(.)"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>


  <xsl:template match="g:body/g:file" mode="request">

    <!-- Data file is read at top level. We provide a parameter here
    to disambiguate the POST call if per-request headers are
    specified.-->
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="concat('request', ../../@request-id, '.data')"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>

 </xsl:template>


  <xsl:template match="g:body/g:form" mode="request">
    <xsl:text>,</xsl:text>
    <xsl:call-template name="tuple-list"/>
  </xsl:template>


  <xsl:template match="g:body/g:string" mode="request">
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="helper:quoteForPython(.)"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>

  <xsl:template match="g:body/g:content-type" mode="request"/>

  <xsl:template match="g:headers[node()]" mode="request">
    <xsl:if test="not(../g:url/g:query-string/g:parsed|../g:body)">
      <!-- No keyword arguments for methods, insert dummy parameter. -->
      <xsl:text>, ()</xsl:text>
    </xsl:if>

    <xsl:text>,</xsl:text>

    <xsl:call-template name="tuple-list"/>
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

  <xsl:template name="tuple-list">
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>(</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>

    <xsl:apply-templates mode="tuple"/>

    <xsl:text> )</xsl:text>
    <xsl:value-of select="helper:changeIndent(-2)"/>
  </xsl:template>

  <xsl:template name="indent-tuple-entry">
    <xsl:param name="first-entry" select="not(preceding-sibling::*)"/>

    <xsl:choose>
      <xsl:when test="$first-entry"><xsl:text> </xsl:text></xsl:when>
      <xsl:otherwise><xsl:value-of select="helper:newLineAndIndent()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="indent">
    <xsl:param name="characters" select="1"/>
    <xsl:value-of select="substring('                      ', 0, $characters)"/>
  </xsl:template>


  <xsl:template match="text()|@*"/>
  <xsl:template match="text()|@*" mode="__call__"/>
  <xsl:template match="text()|@*" mode="file"/>
  <xsl:template match="text()|@*" mode="page"/>
  <xsl:template match="text()|@*" mode="pageDescription"/>
  <xsl:template match="text()|@*" mode="request"/>
  <xsl:template match="text()|@*" mode="TestRunner"/>

</xsl:stylesheet>
