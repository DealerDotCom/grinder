// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

import junit.framework.TestCase;

import javax.swing.SwingUtilities;

import net.grinder.console.communication.ProcessStatus;
import net.grinder.console.communication.ProcessStatus.ProcessReports;


/**
 * Unit tests for {@link SwingDispatchedProcessStatusListener}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestSwingDispatchedProcessStatusListener extends TestCase {

  private Runnable m_voidRunnable = new Runnable() { public void run() {} };

  public void testDispatch() throws Exception {
    final MyProcessStatusListener myListener = new MyProcessStatusListener();
    final ProcessStatus.Listener listener = myListener;

    final ProcessStatus.Listener swingDispatchedListener =
      new SwingDispatchedProcessStatusListener(myListener);

    final ProcessReports[] processReports = new ProcessReports[0];

    listener.update(processReports, true);

    // Wait for a dummy event to be processed by the swing event
    // queue.
    SwingUtilities.invokeAndWait(m_voidRunnable);

    assertTrue(myListener.m_updateCalled);
    assertEquals(processReports, myListener.m_processReports);
    assertTrue(myListener.m_newAgent);

    listener.update(processReports, false);

    // Wait for a dummy event to be processed by the swing event
    // queue.
    SwingUtilities.invokeAndWait(m_voidRunnable);

    assertFalse(myListener.m_newAgent);
  }

  private class MyProcessStatusListener implements ProcessStatus.Listener {
    public boolean m_updateCalled = false;
    public ProcessReports[] m_processReports;
    public boolean m_newAgent;

    public void update(ProcessReports[] processReports, boolean newAgent) {
      m_updateCalled = true;
      m_processReports = processReports;
      m_newAgent = newAgent;
    }
  }
}

