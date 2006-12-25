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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import junit.framework.TestCase;


/**
 * Unit tests for {@link MnemonicHeuristics}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestMnemonicHeuristicsImplementation extends TestCase {


  public void testMnemonicHeuristics() throws Exception {
    final JMenu menu = new JMenu();
    final JButton existing1 = new JButton("Hello");
    existing1.setMnemonic('H');
    menu.add(existing1);
    menu.add(new JLabel("Other stuff"));
    menu.add(new JButton("Goodbye"));
    menu.validate();

    final Expectation[] items = {
        new Expectation("Hello", 'L'),
        new Expectation("", 0),
        new Expectation("Lovely", 'V'),
        new Expectation("Hello", 'E'),
        new Expectation("Hello", 'O'),
        new Expectation("Hello", 0),
        new Expectation("In 1974", 'I'),
        new Expectation("I was", 'W'),
        new Expectation("hospitalised for", 'S'),
        new Expectation("approaching perfection", 'A'),
        new Expectation("baby, there's no guidance", 'B'),
        new Expectation("When Random Rules", 'R'),
        new Expectation("but before you go I've got to ask", 'T'),
        new Expectation("You dear about that tan line on your", 'Y'),
        new Expectation("ring finger", 'N'),
        new Expectation("Random Rules", 'D'),
        new Expectation("Rüles that are Random", 'M'),
    };

    new MnemonicHeuristics(menu);

    for (int i = 0; i < items.length; ++i) {
      final JButton button = new JButton(items[i].getText());
      menu.add(button);

      assertEquals(items[i].toString(),
        items[i].getMnemonic(), button.getMnemonic());
    }
  }

  public void testDynamicBehaviour() throws Exception {
    final JMenuBar menubar = new JMenuBar();

    final JButton b1 = new JButton("Hello");
    final JButton b2 = new JButton("Hello");

    menubar.add(b1);
    menubar.add(b2);

    new MnemonicHeuristics(menubar);

    assertEquals('H', b1.getMnemonic());
    assertEquals('L', b2.getMnemonic());

    b1.setText("Foo");

    assertEquals('F', b1.getMnemonic());
    assertEquals('L', b2.getMnemonic());

    b1.setText("Hello");

    assertEquals('H', b1.getMnemonic());
    assertEquals('L', b2.getMnemonic());

    menubar.add(new JLabel("Something else"));

    assertEquals('H', b1.getMnemonic());
    assertEquals('L', b2.getMnemonic());

    menubar.remove(b1);

    // b1 no longer part of menubar, updating text should not change mnemonic.
    b1.setText("Foo");
    assertEquals('H', b1.getMnemonic());
    assertEquals('L', b2.getMnemonic());
  }

  private class Expectation {
    private final String m_text;
    private final int m_mnemonic;

    public Expectation(String text, int mnemonic) {
      m_text = text;
      m_mnemonic = mnemonic;
    }

    public String getText() {
      return m_text;
    }

    public int getMnemonic() {
      return m_mnemonic;
    }

    public String toString() {
      return "Expectation(\"" + m_text + "\", '" +
                               (char)m_mnemonic + "' (" + m_mnemonic + ")" +
                        ")";
    }
  }
}
