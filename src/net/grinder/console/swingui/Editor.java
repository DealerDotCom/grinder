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

import java.awt.Dimension;
import java.io.File;
import javax.swing.JComponent;

import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.SyntaxStyle;
import org.syntax.jedit.TextAreaPainter;
import org.syntax.jedit.tokenmarker.PythonTokenMarker;
import org.syntax.jedit.tokenmarker.Token;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.console.model.editor.Buffer;
import net.grinder.console.model.editor.EditorModel;
import net.grinder.console.model.editor.TextSource;


/**
 * Text editor.
 *
 * @author Philip Aston
 * @version $Revision$
 */
class Editor {

  private final Resources m_resources;
  private final EditorModel m_editorModel = new EditorModel();
  private final TextSource m_textSource = new JEditSyntaxTextSource();
  private final JEditTextArea m_scriptTextArea;

  /**
   * Constructor.
   *
   * @param resources Console resources.
   */
  public Editor(Resources resources) {
    m_resources = resources;
    m_scriptTextArea = new JEditTextArea();
    m_scriptTextArea.setTokenMarker(new PythonTokenMarker());

    // Override ugly default colours.
    final TextAreaPainter painter = m_scriptTextArea.getPainter();

    final SyntaxStyle[] styles = painter.getStyles();
    styles[Token.KEYWORD1] = new SyntaxStyle(Colours.RED, false, false);
    styles[Token.KEYWORD2] = styles[Token.KEYWORD1];
    styles[Token.KEYWORD3] = styles[Token.KEYWORD1];
    styles[Token.COMMENT1] = new SyntaxStyle(Colours.DARK_GREEN, true, false);
    styles[Token.LITERAL1] = new SyntaxStyle(Colours.BLUE, false, false);
    styles[Token.LITERAL2] = styles[Token.LITERAL1];

    painter.setCaretColor(Colours.DARK_RED);
    painter.setLineHighlightColor(Colours.FAINT_YELLOW);
    painter.setBracketHighlightColor(Colours.GREY);
    painter.setSelectionColor(Colours.GREY);

    // Initial focus?

    m_scriptTextArea.setMinimumSize(new Dimension(200, 100));

    // 1.4 only - use reflection to call this?
    //m_scriptTextArea.setDragEnabled(true);

    final String defaultText =
      m_resources.getStringFromFile("scriptSupportUnderConstruction.text",
                                    true);

    m_textSource.setText(defaultText);
    m_scriptTextArea.setFirstLine(0);

    m_editorModel.setCurrentBuffer(new Buffer(m_resources, m_textSource));
  }

  /**
   * Return the text editing component.
   *
   * @return The component.
   */
  public JComponent getComponent() {
    return m_scriptTextArea;
  }

  public void newFileSelection(File file) throws ConsoleException {
    final Buffer buffer = new Buffer(m_resources, m_textSource, file);
    System.err.println("Loading " + file);
    buffer.load();
    System.err.println("Done loading " + file);

    m_editorModel.setCurrentBuffer(buffer);
    m_scriptTextArea.setFirstLine(0);

  }

  private final class JEditSyntaxTextSource implements TextSource {
    public String getText() {
      return m_scriptTextArea.getText();
    }

    public void setText(String text) {
      m_scriptTextArea.setText(text);
    }

    public int getRevision() {
      // TODO.
      return 1;
    }
  }
}
