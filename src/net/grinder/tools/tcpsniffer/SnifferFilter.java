// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000, 2001, 2002 Philip Aston
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

package net.grinder.tools.tcpsniffer;

import java.io.PrintWriter;


/**
 * Interface that TCP Sniffer filters implement.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public interface SnifferFilter
{
    /**
     * Set the {@link PrintWriter} that the filter should use for
     * output.
     *
     * @param outputPrintWriter a <code>PrintWriter</code> value
     */
    void setOutputPrintWriter(PrintWriter outputPrintWriter);

    /**
     * Handle a message fragment.
     *
     * @param connectionDetails a <code>ConnectionDetails</code> value
     * @param buffer a <code>byte[]</code> value
     * @param bytesRead an <code>int</code> value
     * @return Filters can optionally return a <code>byte[]</code>
     * which will be transmitted to the server instead of
     * <code>buffer</code.
     * @exception Exception if an error occurs
     */
    byte[] handle(ConnectionDetails connectionDetails, byte[] buffer,
		  int bytesRead)
	throws Exception;

    /**
     * A new connection has been opened.
     *
     * @param connectionDetails a <code>ConnectionDetails</code> value
     * @exception Exception if an error occurs
     */
    void connectionOpened(ConnectionDetails connectionDetails)
	throws Exception;

    /**
     * A connection has been closed.
     *
     * @param connectionDetails a <code>ConnectionDetails</code> value
     * @exception Exception if an error occurs
     */
    void connectionClosed(ConnectionDetails connectionDetails)
	throws Exception;
}



