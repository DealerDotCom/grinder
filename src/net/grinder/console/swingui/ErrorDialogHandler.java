// Copyright (C)  2003 Philip Aston
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

import java.awt.Component;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import net.grinder.console.common.ExceptionHandler;
import net.grinder.console.common.Resources;


/**
 * Wrap up all the machinery used to show an error dialog.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class ErrorDialogHandler implements ExceptionHandler {

  private final Component m_parentComponent;
  private final Resources m_resources;
  private final JOptionPane m_optionPane;
  private final JOptionPaneDialog m_dialog;

  private Throwable m_throwable;

  /**
   * Constructor.
   *
   * @param frame Parent frame.
   * @param resources Resources object to use for strings and things.
   **/
  public ErrorDialogHandler(JFrame frame, final Resources resources) {

    m_parentComponent = frame;
    m_resources = resources;
    m_optionPane = createOptionPane(resources);

    m_dialog = new JOptionPaneDialog(frame, m_optionPane, null, true) {
        protected boolean shouldClose() {
          return ErrorDialogHandler.this.shouldClose();
        }
      };
  }

  /**
   * Constructor.
   *
   * @param frame Parent frame.
   * @param resources Resources object to use for strings and things.
   **/
  public ErrorDialogHandler(JDialog dialog, final Resources resources) {

    m_parentComponent = dialog;
    m_resources = resources;
    m_optionPane = createOptionPane(resources);

    m_dialog = new JOptionPaneDialog(dialog, m_optionPane, null, true) {
        protected boolean shouldClose() {
          return ErrorDialogHandler.this.shouldClose();
        }
      };
  }

  private JOptionPane createOptionPane(Resources resources) {

    final Object[] options = {
      resources.getString("error.ok.label"),
      resources.getString("error.details.label"),
    };

    return new JOptionPane(null, JOptionPane.ERROR_MESSAGE, 0, null, options);
  }

  private boolean shouldClose() {
    final Object value = m_optionPane.getValue();

    if (value == m_optionPane.getOptions()[1]) {
      // Details.
      final StringWriter stringWriter = new StringWriter();
      final PrintWriter writer = new PrintWriter(stringWriter);

      m_throwable.printStackTrace(writer);

      // For now just pop up another message dialog.
      JOptionPane.showMessageDialog(
        m_dialog, stringWriter.toString(),
        m_resources.getString("errorDetails.title"),
        JOptionPane.INFORMATION_MESSAGE);

      return false;
    }

    // OK
    return true;
  }

  /**
   * Show the dialog.
   *
   */
  public void exceptionOccurred(Exception exception) {
    exceptionOccurred(exception, m_resources.getString("error.title"));
  }

  /**
   * Show the dialog.
   *
   */
  public void exceptionOccurred(Throwable throwable, String title) {

    m_throwable = throwable;
    m_optionPane.setMessage(m_throwable.getMessage());

    m_dialog.setTitle(title);
    m_dialog.pack();
    m_dialog.setLocationRelativeTo(m_parentComponent);
    m_dialog.setVisible(true);
  }
}
