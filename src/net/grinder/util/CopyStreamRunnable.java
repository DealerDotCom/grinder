// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

package net.grinder.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Runnable that actively copies from an <code>InputStream</code> to
 * an <code>OutputStream</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class CopyStreamRunnable implements Runnable {

  private final InputStream m_in;
  private final OutputStream m_out;

  /**
   * Constructor.
   *
   * @param in Input stream.
   * @param out Output stream.
   */
  public CopyStreamRunnable(InputStream in, OutputStream out) {
    m_in = in;
    m_out = out;
  }

  /**
   * The entry point. Copies from the input stream to the output
   * stream until one of the streams reports an error.
   */
  public void run() {
    final byte[] buffer = new byte[4096];

    try {
      short idle = 0;

      while (true) {
	final int bytesRead = m_in.read(buffer, 0, buffer.length);

	if (bytesRead ==  -1) {
	  break;
	}

	if (bytesRead == 0) {
	  idle++;
	}
	else {
	  m_out.write(buffer, 0, bytesRead);
	  idle = 0;
	}

	if (idle > 0) {
	  Thread.sleep(Math.max(idle * 200, 2000));
	}
      }
    }
    catch (IOException e) {
      // Be silent about IOExceptions ...
    }
    catch (InterruptedException e) {
      // ... and InterruptedExceptions.
    }

    // We're exiting, usually because the in stream has been
    // closed. Whatever, close our streams. This will cause the
    // paired thread to exit too.
    try {
      m_out.close();
    }
    catch (IOException e) {
      // Ignore.
    }

    try {
      m_in.close();
    }
    catch (IOException e) {
      // Ignore.
    }
  }
}
