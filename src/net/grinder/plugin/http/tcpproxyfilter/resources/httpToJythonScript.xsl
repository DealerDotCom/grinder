<?xml version="1.0" encoding="UTF-8"?>

<!--
 Copyright (C) 2006 Philip Aston
 All rights reserved.

 This file is part of The Grinder software distribution. Refer to
 the file LICENSE which is part of The Grinder distribution for
 licensing details. The Grinder distribution is available on the
 Internet at http:grinder.sourceforge.net/

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


<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:g="http://grinder.sourceforge.net/tcpproxy/http/1.0"
  xmlns:helper="net.grinder.plugin.http.tcpproxyfilter.XSLTHelper">

  <xsl:output method="text"/>
  <xsl:strip-space elements="*"/>


  <xsl:template match="g:http-recording">
    <xsl:value-of select="helper:resetIndent()"/>
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
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>class TestRunner:</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"""A TestRunner instance is created for each worker thread."""</xsl:text>
    <xsl:value-of select="helper:newLine()"/>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>def __call__(self):</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"""This method is called for every run performed by the worker thread."""</xsl:text>
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


  <xsl:template match="g:request" mode="generate-test-number">
    <!--  We ignore the @request-id attribute, and calculate our own numbers
          sequentially follow the requests page test number. -->

    <xsl:variable name="request-number"
                  select="count(preceding-sibling::g:request) + 1"/>

    <xsl:variable name="page-test-number">
      <xsl:apply-templates select=".." mode="generate-test-number"/>
    </xsl:variable>

    <xsl:value-of select="$page-test-number + $request-number"/>
  </xsl:template>


  <xsl:template match="g:request" mode="file">
    <xsl:variable name="request-number">
      <xsl:apply-templates select ="." mode="generate-test-number"/>
    </xsl:variable>
    <xsl:variable name="request-name" select="concat('request', $request-number)"/>

    <xsl:if test="not(preceding::g:request)">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># Create an HTTPRequest for each request, then replace the</xsl:text>
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># reference to the HTTPRequest with an instrumented version.</xsl:text>
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># You can access the unadorned instance using </xsl:text>
      <xsl:value-of select="$request-name"/>
      <xsl:text>.__target__.</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="$request-name"/>

    <xsl:value-of select="concat(' = HTTPRequest(url=', g:url/@extends)"/>
    <xsl:value-of select="concat(', headers=', g:headers/@extends, ')')"/>

    <xsl:if test="g:body/g:file">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:value-of select="$request-name"/>
      <xsl:text>.setDataFromFile('</xsl:text>
      <xsl:value-of select="g:body/g:file"/>
      <xsl:text>')</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="concat($request-name, ' = Test(')"/>
    <xsl:value-of select="concat($request-number, ', ')"/>
    <xsl:value-of select="helper:quoteForPython(g:description)"/>
    <xsl:value-of select="concat(').wrap(', $request-name, ')')"/>

    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:request[position() = 1 and position() = last()]" mode="page-description">
    <xsl:value-of select="g:description"/>
  </xsl:template>

  <xsl:template match="g:request[position() = 1]" mode="page-description">
    <xsl:value-of select="g:description"/>

    <xsl:variable name="request-number">
      <xsl:apply-templates select ="." mode="generate-test-number"/>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="position() = last()">
        <xsl:value-of select="concat(' (request ', $request-number, ')')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat(' (requests ', $request-number, '-')"/>
        <xsl:apply-templates select ="following-sibling::g:request[position()=last()]" mode="generate-test-number"/>
        <xsl:text>)</xsl:text>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>


  <xsl:template match="g:request" mode="page-function">
    <xsl:apply-templates select="g:sleep-time" mode="request"/>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:if test="position() = 1">
      <xsl:text>result = </xsl:text>
    </xsl:if>
    <xsl:text>request</xsl:text>
    <xsl:apply-templates select="." mode="generate-test-number"/>
    <xsl:text>.</xsl:text>
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


  <!-- Spacing for page test numbers. -->
  <xsl:variable name="page-test-number-increment" select="100"/>


  <xsl:template match="g:page" mode="generate-number">
    <!--  We ignore the @page-id attribute, and calculate our own number.
          (@page-id is zero-based, we want something that maps to the test
           number).
          We number page tests 100, 200, ... and request tests 101, 102; 201,
          202, ... There's a correspondance between request and page test
          numbers (request test numbers sequentially follow their page Test
          number). We cope gracefully with pages that have more than 100 tests.
          The page number is the page test number / 100.
     -->

    <xsl:value-of select=
      "count(preceding::g:page) +
       count(
         preceding::g:page/g:request[position() mod $page-test-number-increment = 0])
       + 1"/>
  </xsl:template>


  <xsl:template match="g:page" mode="generate-test-number">
    <!--  We ignore the @page-id attribute, and calculate our own number. -->

    <xsl:variable name="page-number">
      <xsl:apply-templates select="." mode="generate-number"/>
    </xsl:variable>

    <xsl:value-of select="$page-number * $page-test-number-increment"/>
  </xsl:template>


  <xsl:template match="g:page" mode="generate-function-name">
    <xsl:text>page</xsl:text>
    <xsl:apply-templates select="." mode="generate-number"/>
  </xsl:template>


  <xsl:template match="g:page" mode="file">
    <xsl:apply-templates select="*" mode="file"/>

    <xsl:variable name="page-number">
      <xsl:apply-templates select="." mode="generate-number"/>
    </xsl:variable>

    <xsl:variable name="page-test-number">
      <xsl:apply-templates select="." mode="generate-test-number"/>
    </xsl:variable>

    <xsl:variable name="page-function-name">
      <xsl:apply-templates select="." mode="generate-function-name"/>
    </xsl:variable>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="concat('def ', $page-function-name, '():')"/>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"""</xsl:text>
    <xsl:apply-templates select="*" mode="page-description"/>
    <xsl:text>."""</xsl:text>
    <xsl:apply-templates select="*" mode="page-function"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>return result</xsl:text>
    <xsl:value-of select="helper:changeIndent(-1)"/>
    <xsl:value-of select="helper:newLine()"/>

    <xsl:if test="not(preceding::g:page)">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># Replace the function with an instrumented version.</xsl:text>
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># You can call the unadorned function using </xsl:text>
      <xsl:value-of select="$page-function-name"/>
      <xsl:text>.__target__().</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="$page-function-name"/>
    <xsl:text> = Test(</xsl:text>
    <xsl:value-of select="$page-test-number"/>
    <xsl:text>, 'Page </xsl:text>
    <xsl:value-of select="$page-number"/>
    <xsl:text>').wrap(</xsl:text>
    <xsl:value-of select="$page-function-name"/>
    <xsl:text>)</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent()"/>
  </xsl:template>


  <xsl:template match="g:page" mode="__call__">
    <xsl:apply-templates select="*" mode="__call__"/>

    <xsl:variable name="page-function-name">
      <xsl:apply-templates select="." mode="generate-function-name"/>
    </xsl:variable>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="concat($page-function-name, '()')"/>
    <xsl:call-template name="indent">
      <xsl:with-param name="characters" select="12-string-length($page-function-name)"/>
    </xsl:call-template>
    <xsl:text># </xsl:text>
    <xsl:apply-templates select="*" mode="page-description"/>
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
    <xsl:text>request</xsl:text>
    <xsl:apply-templates select="../.." mode="generate-test-number"/>
    <xsl:text>.__target__.data'</xsl:text>
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
  <xsl:template match="text()|@*" mode="page-function"/>
  <xsl:template match="text()|@*" mode="page-description"/>
  <xsl:template match="text()|@*" mode="request"/>
  <xsl:template match="text()|@*" mode="TestRunner"/>

</xsl:stylesheet>
