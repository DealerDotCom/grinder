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
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.SyntaxDocument;
import org.syntax.jedit.SyntaxStyle;
import org.syntax.jedit.TextAreaDefaults;
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
import net.grinder.console.model.editor.Buffer;
import net.grinder.console.model.editor.TextSource;


/**
 * Text editor.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class Editor {

  private final Resources m_resources;
  private final JEditTextArea m_scriptTextArea;
  private final TitledBorder m_titledBorder;

  /**
   * Constructor.
   *
   * @param resources Console resources.
   */
  public Editor(Resources resources, TitledBorder titledBorder)
    throws ConsoleException {

    m_resources = resources;
    m_titledBorder = titledBorder;

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

    m_scriptTextArea = new JEditTextArea(textAreaDefaults) {
        public Dimension getPreferredSize() {
          final Dimension parentSize = getParent().getSize();
          final Insets insets = getParent().getInsets();

          return new Dimension(parentSize.width - insets.left - insets.right,
                               parentSize.height - insets.top - insets.bottom);
        }
      };

    // Initial focus?

    newFileSelection(null);
  }

  /**
   * Return the text editing component.
   *
   * @return The component.
   */
  public JComponent getComponent() {
    return m_scriptTextArea;
  }

  public void newFileSelection(FileTreeModel.FileNode fileNode)
    throws ConsoleException {

    final Buffer buffer;

    if (fileNode != null) {
      if (fileNode.getBuffer() == null) {
        buffer = new Buffer(m_resources,
                            new JEditSyntaxTextSource(fileNode),
                            fileNode.getFile());
        buffer.load();
        fileNode.setBuffer(buffer);
      }
      else {
        buffer = fileNode.getBuffer();
      }

      m_titledBorder.setTitle(fileNode.getFile().getPath());
    }
    else {
      final TextSource textSource =
        new JEditSyntaxTextSource(
          m_resources.getStringFromFile(
            "scriptSupportUnderConstruction.text", true));

      buffer = new Buffer(m_resources, textSource);

      m_titledBorder.setTitle(m_resources.getString("editor.title"));
    }

    buffer.setActive();

    m_scriptTextArea.setBorder(m_titledBorder);
    m_scriptTextArea.setCaretPosition(0);
    m_scriptTextArea.setTokenMarker(getTokenMarker(buffer.getType()));

    // Repaint so the border is updated.
    m_scriptTextArea.repaint();
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

  private class JEditSyntaxTextSource implements TextSource {

    private final FileTreeModel.FileNode m_fileNode;
    private final SyntaxDocument m_syntaxDocument = new SyntaxDocument();
    private boolean m_dirty;

    public JEditSyntaxTextSource(FileTreeModel.FileNode fileNode) {

      m_fileNode = fileNode;

      m_syntaxDocument.addDocumentListener(
        new DocumentListener() {
          public void insertUpdate(DocumentEvent event) { m_dirty = true; }
          public void removeUpdate(DocumentEvent event) { m_dirty = true; }
          public void changedUpdate(DocumentEvent event) { m_dirty = true; }
        });
    }

    public JEditSyntaxTextSource(String text) {
      this((FileTreeModel.FileNode)null);

      setText(text);
    }

    public String getText() {
      m_dirty = false;

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

      m_dirty = false;
    }

    public boolean isDirty() {
      return m_dirty;
    }

    public void setActive() {
      m_scriptTextArea.setDocument(m_syntaxDocument);
    }
  }
}
