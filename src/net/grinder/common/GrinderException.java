// Copyright (C) 2000 Paco Gomez
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

package net.grinder.common;

import java.io.PrintStream;
import java.io.PrintWriter;


/**
 * GrinderException.java
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class GrinderException extends Exception
{
    private final Exception m_nestedException;

    public GrinderException(String message)
    {
	this(message, null);
    }

    public GrinderException(String message, Exception nestedException)
    {
	super(message);
	m_nestedException = nestedException;
    }
    
    public String toString()
    {
	return getClass().getName() + ": " + getMessage() +
	    (m_nestedException != null ? 
	     ", nested exception: " + m_nestedException : "");
    }


    public void printStackTrace(PrintWriter s)
    {
	super.printStackTrace(s);

	if (m_nestedException != null) {
	    s.print("\n\tNested exception stack trace: ");
	    m_nestedException.printStackTrace(s);
	}

	s.flush();
    }

    public void printStackTrace()
    {
	printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream s)
    {
	printStackTrace(new PrintWriter(s));
    }

    public Exception getNestedException()
    {
	return m_nestedException;
    }
}
