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

package net.grinder.console.swingui;

import javax.swing.SwingUtilities;

import net.grinder.common.ProcessStatus;
import net.grinder.console.model.ProcessStatusSetListener;


/**
 * ProcessStatusSetListener Decorator that disptaches the update()
 * notifications via a Swing thread.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
class SwingDispatchedProcessStatusSetListener
    implements ProcessStatusSetListener
{
    private final ProcessStatusSetListener m_delegate;

    public SwingDispatchedProcessStatusSetListener(
	ProcessStatusSetListener delegate)
    {
	m_delegate = delegate;
    }

    public void update(final ProcessStatus[] data, final int running,
		       final int total)
    {
	SwingUtilities.invokeLater(
	    new Runnable() {
		public void run() { m_delegate.update(data, running, total); }
	    }
	    );
    }
}
