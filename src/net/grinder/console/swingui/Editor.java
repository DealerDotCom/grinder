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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.SyntaxDocument;
import org.syntax.jedit.SyntaxStyle;
import org.syntax.jedit.TextAreaDefaults;
import org.syntax.jedit.TextAreaPainter;
import org.syntax.jedit.tokenmarker.BatchFileTokenMarker;
import org.syntax.jedit.tokenmarker.HTMLTokenMarker;
import org.syntax.jedit.tokenmarker.JavaTokenMarker;
import org.syntax.jedit.tokenmarker.PropsTokenMarker;
import org.syntax.jedit.tokenmarker.PythonTokenMarker;
import org.syntax.jedit.tokenmarker.ShellScriptTokenMarker;
import org.syntax.jedit.tokenmarker.Token;
import org.syntax.jedit.tokenmarker.TokenMarker;
import org.syntax.jedit.tokenmarker.XMLTokenMarker;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.console.editor.AbstractTextSource;
import net.grinder.console.editor.Buffer;
import net.grinder.console.editor.EditorModel;
import net.grinder.console.editor.TextSource;


/**
 * Text editor.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class Editor {

  private final EditorModel m_editorModel;
  private final CustomJEditTextArea m_scriptTextArea;
  private final TitledBorder m_titledBorder;
  private final Font m_noFileTitleFont;
  private final Font m_unmodifiedFileTitleFont;
  private final Font m_modifiedFileTitleFont;

  /**
   * Constructor.
   *
   * @param resources Console resources.
   */
  public Editor(final Resources resources, EditorModel editorModel,
                Font tabLabelFont)
    throws ConsoleException {

    m_editorModel = editorModel;
    m_noFileTitleFont = tabLabelFont;

    final TextAreaDefaults textAreaDefaults = TextAreaDefaults.getDefaults();

    // Override ugly default colours.
    final SyntaxStyle[] styles = textAreaDefaults.styles;
    styles[Token.KEYWORD1] = new SyntaxStyle(Colours.RED, false, false);
    styles[Token.KEYWORD2] = styles[Token.KEYWORD1];
    styles[Token.KEYWORD3] = styles[Token.KEYWORD1];
    styles[Token.COMMENT1] = new SyntaxStyle(Colours.DARK_GREEN, true, false);
    styles[Token.LITERAL1] = new SyntaxStyle(Colours.BLUE, false, false);
    styles[Token.LITERAL2] = styles[Token.LITERAL1];

    textAreaDefaults.caretColor = Colours.DARK_RED;
    textAreaDefaults.lineHighlightColor = Colours.FAINT_YELLOW;
    textAreaDefaults.bracketHighlightColor = Colours.GREY;
    textAreaDefaults.selectionColor = Colours.GREY;
    textAreaDefaults.cols = 1;

    m_scriptTextArea = new CustomJEditTextArea(textAreaDefaults);

    m_titledBorder =
      BorderFactory.createTitledBorder(
        BorderFactory.createEmptyBorder(0, 1, 3, 1), "x");

    m_unmodifiedFileTitleFont = m_noFileTitleFont.deriveFont(Font.BOLD);
    m_modifiedFileTitleFont =
      m_noFileTitleFont.deriveFont(Font.ITALIC | Font.BOLD);

    m_titledBorder.setTitleColor(Colours.HIGHLIGHT_TEXT);
    m_titledBorder.setTitleJustification(TitledBorder.RIGHT);

    m_scriptTextArea.setBorder(m_titledBorder);

    m_editorModel.addListener(new EditorModel.AbstractListener() {
        public void bufferStateChanged(Buffer buffer) {
          final Buffer selectedBuffer = m_editorModel.getSelectedBuffer();

          if (selectedBuffer == null) {
            m_titledBorder.setTitle(resources.getString("editor.title"));
            m_titledBorder.setTitleFont(m_noFileTitleFont);

            m_scriptTextArea.setDocument(new SyntaxDocument());
            m_scriptTextArea.setEnabled(false);
          }
          else if (buffer.equals(selectedBuffer)) {
            m_titledBorder.setTitle(buffer.getDisplayName());
            m_titledBorder.setTitleFont(
              buffer.isDirty() ?
              m_modifiedFileTitleFont : m_unmodifiedFileTitleFont);

            final JEditSyntaxTextSource textSource =
              (JEditSyntaxTextSource)buffer.getTextSource();

            m_scriptTextArea.setDocument(textSource.getSyntaxDocument());
            m_scriptTextArea.setTokenMarker(getTokenMarker(buffer.getType()));
            m_scriptTextArea.setEnabled(true);
            m_scriptTextArea.requestFocus();
          }

          // Repaint so the border is updated.
          m_scriptTextArea.repaint();
        }
      });

    m_editorModel.selectDefaultBuffer();
  }

  /**
   * Return the text editing component.
   *
   * @return The component.
   */
  public JComponent getComponent() {
    return m_scriptTextArea;
  }

  private TokenMarker getTokenMarker(Buffer.Type bufferType) {
    if (bufferType == Buffer.HTML_BUFFER) {
      return new HTMLTokenMarker();
    }
    else if (bufferType == Buffer.JAVA_BUFFER) {
      return new JavaTokenMarker();
    }
    else if (bufferType == Buffer.MSDOS_BATCH_BUFFER) {
      return new BatchFileTokenMarker();
    }
    else if (bufferType == Buffer.PROPERTIES_BUFFER) {
      return new PropsTokenMarker();
    }
    else if (bufferType == Buffer.PYTHON_BUFFER) {
      return new PythonTokenMarker();
    }
    else if (bufferType == Buffer.SHELL_BUFFER) {
      return new ShellScriptTokenMarker();
    }
    else if (bufferType == Buffer.TEXT_BUFFER) {
      return null;
    }
    else if (bufferType == Buffer.XML_BUFFER) {
      return new XMLTokenMarker();
    }
    else {
      return null;
    }
  }

  private static class JEditSyntaxTextSource extends AbstractTextSource {

    private final SyntaxDocument m_syntaxDocument = new SyntaxDocument();

    JEditSyntaxTextSource() {

      m_syntaxDocument.addDocumentListener(
        new DocumentListener() {
          public void insertUpdate(DocumentEvent event) { documentChanged(); }
          public void removeUpdate(DocumentEvent event) { documentChanged(); }
          public void changedUpdate(DocumentEvent event) { documentChanged(); }

          private void documentChanged() {
            setChanged();
          }
        });
    }

    SyntaxDocument getSyntaxDocument() {
      return m_syntaxDocument;
    }

    public String getText() {
      setClean();

      try {
        return m_syntaxDocument.getText(0, m_syntaxDocument.getLength());
      }
      catch (BadLocationException bl) {
        bl.printStackTrace();
        return "";
      }
    }

    public void setText(String text) {
      try {
        m_syntaxDocument.beginCompoundEdit();
        m_syntaxDocument.remove(0, m_syntaxDocument.getLength());
        m_syntaxDocument.insertString(0, text, null);
      }
      catch (BadLocationException bl) {
        bl.printStackTrace();
      }
      finally {
        m_syntaxDocument.endCompoundEdit();
      }

      setClean();
    }
  }

  /**
   * Factory for {@link TextSource}s that can be used with {@link
   * Editor}.
   */
  static class TextSourceFactory implements TextSource.Factory {
    public TextSource create() {
      return new JEditSyntaxTextSource();
    }
  }

  /**
   * Fix JEditTextArea annoyances.
   */
  private static final class CustomJEditTextArea extends JEditTextArea {
    private final Color m_enabledBackground;
    private final Color m_enabledCaretColour;

    public CustomJEditTextArea(TextAreaDefaults textAreaDefaults) {
      super(textAreaDefaults);

      final TextAreaPainter painter = getPainter();
      m_enabledBackground = painter.getBackground();
      m_enabledCaretColour = painter.getCaretColor();
    }

    public Dimension getPreferredSize() {
      final Dimension parentSize = getParent().getSize();
      final Insets insets = getParent().getInsets();

      return new Dimension(parentSize.width - insets.left - insets.right,
                           parentSize.height - insets.top - insets.bottom);
    }

    public void setEnabled(boolean b) {
      super.setEnabled(b);

      setEditable(b);

      final TextAreaPainter painter = getPainter();

      // JEditTextArea has a MouseListener that calls requestFocus
      // without checking isRequestFocusEnabled, so there is no way to
      // stop the text area from receiving focus due to a mouse click.
      // We can't use setCaretVisible(false) to make the caret
      // invisible as it is set true by the JEditTextArea focus
      // handler. Paint it invisibly instead.
      painter.setCaretColor(b ? m_enabledCaretColour : Colours.GREY);
      painter.setBackground(b ? m_enabledBackground : Colours.GREY);
      painter.setLineHighlightEnabled(b);

      if (!b) {
        transferFocus();
      }

      setRequestFocusEnabled(b);
    }

    public boolean isFocusable() {
      return isEnabled();
    }
  }
}
