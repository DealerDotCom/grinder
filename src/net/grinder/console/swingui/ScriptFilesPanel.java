// Copyright (C) 2003 Philip Aston
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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.grinder.console.model.ScriptDistributionFiles;


/**
 * @author Philip Aston
 * @version $Revision$
 */
class ScriptFilesPanel extends JPanel {

  private final JLabel m_rootDirectoryLabel = new JLabel();

  private final Resources m_resources;
  private ScriptDistributionFiles m_scriptDistributionFiles = null;

  public ScriptFilesPanel(Resources resources) {
    m_resources = resources;

    final JButton chooseDirectoryButton = new CustomJButton();

    chooseDirectoryButton.setBorderPainted(true);
    chooseDirectoryButton.setBorder(
      BorderFactory.createEmptyBorder(1, 1, 1, 1));

    chooseDirectoryButton.setAction(
      new CustomAction(resources, "script.chooseDirectory") {
	public final void actionPerformed(ActionEvent e) {
	}
      }
      );

    m_rootDirectoryLabel.setBorder(
      BorderFactory.createEmptyBorder(5, 5, 0, 0));

    final Box rootDirectoryPanel = Box.createHorizontalBox();
    rootDirectoryPanel.add(chooseDirectoryButton);
    rootDirectoryPanel.add(m_rootDirectoryLabel);
    //    rootDirectoryPanel.setBorder(
    //      BorderFactory.createTitledBorder(
    //	resources.getString("script.directory.label")));

    setLayout(new GridLayout(0, 1));

    add(rootDirectoryPanel);
  }

  public void set(ScriptDistributionFiles scriptDistributionFiles) {
    
    m_scriptDistributionFiles = null;

    m_rootDirectoryLabel.setText(
      scriptDistributionFiles.getRootDirectory().getPath());

    m_scriptDistributionFiles = scriptDistributionFiles;
  }

  public void refresh() {
    set(m_scriptDistributionFiles);
  }
}

  
