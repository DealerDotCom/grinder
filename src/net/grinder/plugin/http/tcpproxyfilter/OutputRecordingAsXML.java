// Copyright (C) 2005 Philip Aston
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
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.plugin.http.tcpproxyfilter;

import net.grinder.common.Logger;
import net.grinder.plugin.http.xml.HttpRecordingDocument;


/**
 * Output an {@link HTTPRecordingImplementation} result as XML text.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class OutputRecordingAsXML implements HTTPRecordingResultProcessor {

  private final Logger m_logger;

  /**
   * Constructor.
   *
   * @param logger Where to direct the output.
   */
  public OutputRecordingAsXML(Logger logger) {
    m_logger = logger;
  }

  /**
   * Produce output.
   *
   * @param result The result to process.
   */
  public void process(HttpRecordingDocument result) {
    m_logger.getOutputLogWriter().println(result);
    m_logger.getOutputLogWriter().flush();
  }
}
