// Copyright (C) 2003 Bertrand Ave
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

package net.grinder.tools.tcpproxy;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingConstants;


/**
 * Console for the TCPProxy.
 *
 * @author Bertrand Ave
 * @version $Revision$
 */
public final class TCPProxyConsole extends JFrame {

  /**
   * Constructor.
   *
   * @param proxyEngine The <code>TCPProxyEngine</code> we control.
   */
  public TCPProxyConsole(final TCPProxyEngine proxyEngine) {
    super("TCPProxy Console");

    setResizable(false);

    addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          proxyEngine.stop();
        }
      });

    final Container content = getContentPane();
    content.setBackground(Color.white);
    content.setLayout(new FlowLayout());

    final JButton button1 = new JButton("Recording");
    button1.setEnabled(false);
    content.add(button1);

    final JButton button2 = new JButton("Stop");
    button2.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
           proxyEngine.stop();
         }
      });

    button2.setHorizontalTextPosition(SwingConstants.LEFT);
    content.add(button2);

    pack();
    setVisible(true);
  }
}
