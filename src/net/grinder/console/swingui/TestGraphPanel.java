// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002 Philip Aston
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
     * {@link net.grinder.console.model.ModelListener} interface. New
     * <code>StatisticsView</code>s have been added. We need do
     * nothing
     **/
    public void newStatisticsViews(StatisticsView intervalStatisticsView,
				   StatisticsView cumulativeStatisticsView)
    {
    }
}
