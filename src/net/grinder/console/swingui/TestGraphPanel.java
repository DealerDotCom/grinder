// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.console.swingui;

import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.grinder.common.Test;
import net.grinder.console.model.Model;
import net.grinder.console.model.ModelListener;
import net.grinder.console.model.SampleListener;
import net.grinder.statistics.StatisticsView;
import net.grinder.statistics.TestStatistics;


/**
 * A panel of test graphs.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestGraphPanel extends JPanel implements ModelListener
{
    private final Model m_model;
    private final Resources m_resources;
    private final String m_testLabel;

    /**
     * Map of {@link net.grinder.common.Test}s to {@link
     * javax.swing.JComponent}s.
     **/
    private final Map m_components = new HashMap();

    private boolean m_modelReset = true;

    TestGraphPanel(final Model model, Resources resources)
    {
	setLayout(new GridLayout(0, 2, 20, 0));
	m_model = model;
	m_resources = resources;

	m_testLabel = resources.getString("graph.test.label") + " ";

	m_model.addModelListener(new SwingDispatchedModelListener(this));

	m_model.addTotalSampleListener(
	    new SampleListener() {
		public void update(TestStatistics intervalStatistics,
				   TestStatistics cumulativeStatistics) {
		    // No requirement to dispatch in Swing thread.
		    LabelledGraph.resetPeak();
		}
	    });
    }

    public void reset(Set newTests)
    {
	m_modelReset = true;

	final Iterator newTestIterator = newTests.iterator();
	
	while (newTestIterator.hasNext()) {
	    final Test test = (Test)newTestIterator.next();

	    final String description = test.getDescription();

	    final String label =
		m_testLabel + test.getNumber() +
		(description != null ? " (" + description + ")" : "");

	    final LabelledGraph testGraph =
		new LabelledGraph(label, m_resources,
				  m_model.getTPSExpression(),
				  m_model.getPeakTPSExpression());

	    m_model.addSampleListener(
		test,
		new SwingDispatchedSampleListener(
		    new SampleListener() {
			public void update(
			    final TestStatistics intervalStatistics,
			    final TestStatistics cumulativeStatistics) {
			    testGraph.add(intervalStatistics,
					  cumulativeStatistics,
					  m_model.getNumberFormat());
			    
			}
		    }));

	    m_components.put(test, testGraph);
	}
    }

    public void update()
    {
	if (m_modelReset) {
	    m_modelReset = false;

	    final int numberOfTests = m_model.getNumberOfTests();

	    // We add all the tests components again. The container
	    // ignores duplicates, but inserts the new components in
	    // the correct order.
	    for (int i=0; i<numberOfTests; i++) {
		add((JComponent)m_components.get(m_model.getTest(i)));
	    }
	}
    }

    /**
     * {@link ModelListener} interface. New
     * <code>StatisticsView</code>s have been added. We need do
     * nothing
     **/
    public void newStatisticsViews(StatisticsView intervalStatisticsView,
				   StatisticsView cumulativeStatisticsView)
    {
    }
}
