// Copyright (C) 2001, 2002, 2003, 2004 Philip Aston
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.grinder.common.Test;
import net.grinder.console.common.Resources;
import net.grinder.console.model.Model;
import net.grinder.console.model.ModelListener;
import net.grinder.console.model.ModelTestIndex;
import net.grinder.console.model.SampleListener;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatistics;


/**
 * A panel of test graphs.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestGraphPanel extends JPanel implements ModelListener {

  private final JComponent m_parentComponent;
  private final BorderLayout m_borderLayout = new BorderLayout();
  private final FlowLayout m_flowLayout = new FlowLayout(FlowLayout.LEFT);
  private final JLabel m_logoLabel;

  private final Dimension m_preferredSize = new Dimension();

  private final Model m_model;
  private final Resources m_resources;
  private final String m_testLabel;

  /**
   * Map of {@link net.grinder.common.Test}s to {@link
   * javax.swing.JComponent}s.
   **/
  private final Map m_components = new HashMap();

  TestGraphPanel(final JComponent parentComponent, final Model model) {

    m_parentComponent = parentComponent;

    m_model = model;
    m_resources = model.getResources();

    m_testLabel = m_resources.getString("graph.test.label") + " ";

    m_model.addModelListener(new SwingDispatchedModelListener(this));

    m_model.addTotalSampleListener(
      new SampleListener() {
        public void update(TestStatistics intervalStatistics,
                           TestStatistics cumulativeStatistics) {
          // No requirement to dispatch in Swing thread.
          LabelledGraph.resetPeak();
        }
      });

    m_logoLabel = new JLabel(m_resources.getImageIcon("logo-large.image"));
  }

  /**
   * Model listener interface called when new tests have been registered.
   *
   * @param newTests The new tests.
   * @param modelTestIndex Updated test index.
   */
  public void newTests(Set newTests, ModelTestIndex modelTestIndex) {

    remove(m_logoLabel);
    setLayout(m_flowLayout);

    final Iterator newTestIterator = newTests.iterator();

    while (newTestIterator.hasNext()) {
      final Test test = (Test)newTestIterator.next();

      final String description = test.getDescription();

      final String label =
        m_testLabel + test.getNumber() +
        (description != null ? " (" + description + ")" : "");

      final LabelledGraph testGraph =
        new LabelledGraph(label, m_resources, m_model.getTPSExpression(),
                          m_model.getPeakTPSExpression());

      m_model.addSampleListener(
        test,
        new SwingDispatchedSampleListener(
          new SampleListener() {
            public void update(final TestStatistics intervalStatistics,
                               final TestStatistics cumulativeStatistics) {
              testGraph.add(intervalStatistics, cumulativeStatistics,
                            m_model.getNumberFormat());
            }
          }));

      m_components.put(test, testGraph);
    }

    final int numberOfTests = modelTestIndex.getNumberOfTests();

    // We add all the tests components again. The container ignores
    // duplicates, but inserts the new components in the correct
    // order.
    for (int i = 0; i < numberOfTests; i++) {
      add((JComponent)m_components.get(modelTestIndex.getTest(i)));
    }

    // Invalidate preferred size cache.
    m_preferredSize.width = -1;

    validate();
  }

  /**
   * Specify our preferred size to prevent our FlowLayout from laying
   * us out horizontally. We fix our width to that of our containing
   * tab, and calculate our vertical height. The intermediate scroll
   * pane uses the preferred size.
   *
   * @return a <code>Dimension</code> value
   */
  public final Dimension getPreferredSize() {

    if (m_components.size() == 0) {
      return super.getPreferredSize();
    }

    // Width is whatever our parent says.
    final Insets parentComponentInsets = m_parentComponent.getInsets();

    final int preferredWidth =
      m_parentComponent.getWidth() -
      parentComponentInsets.left - parentComponentInsets.right;

    if (m_preferredSize.width == preferredWidth) {
      // Nothing's changed.
      return m_preferredSize;
    }

    m_preferredSize.width = preferredWidth;

    // Now ape the FlowLayout algorithm to calculate desired height,
    // *sigh*.
    final int n = getComponentCount();
    final int hgap = m_flowLayout.getHgap();

    final Insets insets = getInsets();

    final int fudgeFactor = 6;    // I've no idea where this extra space
    // comes from, but we need it.

    int availableWidth =
      preferredWidth - insets.left - insets.right - hgap + fudgeFactor;

    if (n > 0) {
      // Assume we have a homogeneous set of fixed size components.
      final int componentWidth = getComponent(0).getWidth();
      final int componentHeight = getComponent(0).getHeight();

      int numberAcross = -1;

      while (componentWidth > 0 && availableWidth > 0) {
        ++numberAcross;
        availableWidth -= componentWidth;
        availableWidth -= hgap;
      }

      if (numberAcross > 0) {
        final int numberDown = (n + numberAcross - 1) / numberAcross;

        // numberDown is always >= 1.
        m_preferredSize.height =
          numberDown * componentHeight +
          (numberDown - 1) * m_flowLayout.getVgap();
      }
    }

    return m_preferredSize;
  }

  /**
   * Called when the model has new information.
   **/
  public final void update() {
  }

  /**
   * {@link net.grinder.console.model.ModelListener} interface. New
   * <code>StatisticsView</code>s have been added. We need do
   * nothing
   *
   * @param intervalStatisticsView Interval statistics view.
   * @param cumulativeStatisticsView Cumulative statistics view.
   */
  public void newStatisticsViews(StatisticsView intervalStatisticsView,
                                 StatisticsView cumulativeStatisticsView) {
  }

  /**
   * {@link net.grinder.console.model.ModelListener} interface.
   * Existing <code>Test</code>s and <code>StatisticsView</code>s have
   * been discarded.
   */
  public final void resetTestsAndStatisticsViews() {
    m_components.clear();
    removeAll();
    setLayout(m_borderLayout);
    add(m_logoLabel, BorderLayout.CENTER);
    invalidate();
  }
}
