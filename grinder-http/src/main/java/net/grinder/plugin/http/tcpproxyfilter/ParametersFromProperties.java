// Copyright (C) 2012 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.plugin.http.tcpproxyfilter;

import static java.util.Arrays.asList;
import java.util.HashSet;
import java.util.Set;


/**
 * Obtain recording parameters from system properties.
 *
 * @author Philip Aston
 */
public class ParametersFromProperties implements HTTPRecordingParameters {

  private static final Set<String> COMMON_HEADERS =
    new HashSet<String>(asList(
          "Accept",
          "Accept-Charset",
          "Accept-Encoding",
          "Accept-Language",
          "Cache-Control",
          "Referer", // Deliberate misspelling to match specification.
          "User-Agent"
      ));

  private static final Set<String> MIRRORED_HEADERS =
    new HashSet<String>(asList(
          "Content-Type",
          "Content-type", // Common misspelling.
          "If-Modified-Since",
          "If-None-Match"
      ));

  static {
    MIRRORED_HEADERS.addAll(COMMON_HEADERS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getTestNumberOffset() {

    final String value = System.getProperty("HTTPPlugin.initialTest");

    if (value != null) {
      return Integer.parseInt(value);
    }

    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isCommonHeader(String headerName) {
    return COMMON_HEADERS.contains(headerName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isMirroredHeader(String headerName) {
    return MIRRORED_HEADERS.contains(headerName);
  }
}
