// Copyright (C) 2004 Philip Aston
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.SwingUtilities;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestSwingDispatchedPropertyChangeListener extends TestCase {

  public TestSwingDispatchedPropertyChangeListener(String name) {
    super(name);
  }

  private Runnable m_voidRunnable = new Runnable() { public void run() {} };

  public void testDispatch() throws Exception {
    final MyPropertyChangeListener listener = new MyPropertyChangeListener();

    final PropertyChangeListener swingDispatchedListener =
      new SwingDispatchedPropertyChangeListener(listener);

    final PropertyChangeEvent event =
      new PropertyChangeEvent(this, "my property", "before", "after");

    listener.propertyChange(event);

    // Wait for a dummy event to be processed by the swing event
    // queue.
    SwingUtilities.invokeAndWait(m_voidRunnable);

    assertEquals(event, listener.m_propertyChangeEvent);
  }

  private class MyPropertyChangeListener implements PropertyChangeListener {
    public PropertyChangeEvent m_propertyChangeEvent;
	
    public void propertyChange(PropertyChangeEvent event) {
      m_propertyChangeEvent = event;
    }
  }
}

