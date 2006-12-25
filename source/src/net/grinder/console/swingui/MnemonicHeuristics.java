// Copyright (C) 2006 Philip Aston
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
import java.awt.Container;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JMenu;


/**
 * Automatically set mnemonics for all {@link AbstractButton}s in a
 * {@link ContainerListener}. Uses <a href=
 * "http://weblogs.java.net/blog/enicholas/archive/2006/06/mnemonic_magic.html">
 * heuristics suggested by Ethan Nichols</a>.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
final class MnemonicHeuristics {

  private final Heuristic[] m_heuristics = {
      new FirstCharacterHeuristic(),
      new UpperCaseHeuristic(),
      new ConsonantHeuristic(),
      new LetterOrDigitHeuristic(),
  };

  private final MnemonicSet m_existingMnemonics = new MnemonicSet();

  private final TextChangedListener m_textChangedListener =
    new TextChangedListener();

  /**
   * Each <code>MnemonicHeuristics</code> is built for a particular container.
   * It sets mnemonics for any {@link AbstractButton}s that already exist in
   * the container, and registers a listener to set mnemonics for new buttons
   * added to the container.
   *
   * <p>
   * Additionally, it watches the text of each button and recalculates mnemonics
   * on changes.
   * </p>
   *
   *
   * @param theContainer
   */
  public MnemonicHeuristics(Container theContainer) {

    // Mutter, mutter. JMenu extends Container and overrides the various
    // flavours of <code>add</code>, but does not delegate to Container
    // implementation, nor override {@link Container#getComponents()}.
    // Hence the need for this hack:
    final Container container;

    if (theContainer instanceof JMenu) {
      container = ((JMenu)theContainer).getPopupMenu();
    }
    else {
      container = theContainer;
    }

    final Component[] existingComponents = container.getComponents();

    for (int i = 0; i < existingComponents.length; ++i) {
      if (existingComponents[i] instanceof AbstractButton) {
        final AbstractButton button = (AbstractButton)existingComponents[i];
        setMnemonic(button);
        m_textChangedListener.add(button);
      }
    }

    container.addContainerListener(new ContainerListener() {
      public void componentAdded(ContainerEvent e) {
        if (e.getChild() instanceof AbstractButton) {
          final AbstractButton button = (AbstractButton)e.getChild();
          setMnemonic(button);
          m_textChangedListener.add(button);
        }
      }

      public void componentRemoved(ContainerEvent e) {
        m_textChangedListener.remove(e.getChild());
      }
    });
  }

  private void setMnemonic(AbstractButton button) {
    final int existingMnemonic = button.getMnemonic();

    if (existingMnemonic != 0) {
      m_existingMnemonics.add(existingMnemonic);
    }
    else {
      final String text = button.getText();

      for (int i = 0; i < m_heuristics.length; ++i) {
        final int result = m_heuristics[i].apply(text);

        if (result != 0) {
          button.setMnemonic(result);
          m_existingMnemonics.add(result);
          return;
        }
      }
    }
  }

  private final class TextChangedListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent evt) {
      final AbstractButton button = (AbstractButton)evt.getSource();
      m_existingMnemonics.remove(button.getMnemonic());
      button.setMnemonic(0);
      setMnemonic(button);
    }

    public void add(Component component) {
      component.addPropertyChangeListener(
        AbstractButton.TEXT_CHANGED_PROPERTY, this);
    }

    public void remove(Component component) {
      component.removePropertyChangeListener(
        AbstractButton.TEXT_CHANGED_PROPERTY, this);
    }
  }

  private int toKey(char c) {
    // We convert candidate characters to key by converting to uppercase...
    final char upper = Character.toUpperCase(c);

    // .. filtering out existing mnemonics...
    if (!m_existingMnemonics.contains(upper)) {

      // .. and throwing away anything that doesn't map to a key.
      if (KeyEvent.getKeyText(upper).equals(String.valueOf(upper))) {
        return upper;
      }
    }

    return 0;
  }

  private interface Heuristic {
    int apply(String text);
  }

  private class FirstCharacterHeuristic implements Heuristic {
    public int apply(String text) {
      return text.length() > 0 ? toKey(text.charAt(0)) : 0;
    }
  }

  private abstract class AbstactEarliestMatchHeuristic implements Heuristic {
    public int apply(String text) {
      final char[] characters = text.toCharArray();

      for (int i = 0; i < characters.length; ++i) {
        if (matches(characters[i])) {
          final int result = toKey(characters[i]);

          if (result != 0) {
            return result;
          }
        }
      }

      return 0;
    }

    protected abstract boolean matches(char c);
  }

  private class UpperCaseHeuristic extends AbstactEarliestMatchHeuristic {
    protected boolean matches(char c) { return Character.isUpperCase(c); }
  }

  private class LetterOrDigitHeuristic extends AbstactEarliestMatchHeuristic {
    protected boolean matches(char c) { return Character.isLetterOrDigit(c); }
  }

  private class ConsonantHeuristic extends LetterOrDigitHeuristic {
    protected boolean matches(char c) {
      // Prioritising consonants is English-centric and rough and ready. I'm
      // not sweating about whether this ought to consider other Unicode
      // characters.
      return
        super.matches(c) &&
        c != 'a' &&
        c != 'e' &&
        c != 'i' &&
        c != 'o' &&
        c != 'u';
    }
  }

  private class MnemonicSet {
    private final Set m_set = new HashSet();

    public boolean add(int mnemonic) {
      return m_set.add(new Integer(mnemonic));
    }

    public boolean remove(int mnemonic) {
      return m_set.remove(new Integer(mnemonic));
    }

    public boolean contains(int mnemonic) {
      return m_set.contains(new Integer(mnemonic));
    }
  }
}
