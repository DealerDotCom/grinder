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
import java.util.Iterator;
import javax.swing.JPanel;

import net.grinder.console.ConsoleException;
import net.grinder.console.model.Model;
import net.grinder.console.model.SampleListener;
import net.grinder.plugininterface.Test;
import net.grinder.statistics.Statistics;


/**
 * A paneL of test graphs.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestGraphPanel extends JPanel
{
    TestGraphPanel(Model model) throws ConsoleException
    {
	setLayout(new GridLayout(0, 2, 20, 0));

	final Iterator testIterator = model.getTests().iterator();

	while (testIterator.hasNext()) {
	    final Test test = (Test)testIterator.next();

	    final Integer testNumber = test.getTestNumber();
	    final String description = test.getDescription();

	    String label = "Test " + testNumber;

	    if (description != null) {
		label = label + " (" + description + ")";
	    }

	    final LabelledGraph testGraph = new LabelledGraph(label);

	    model.addSampleListener(
		testNumber,
		new SampleListener() {
		    public void update(double tps, double averageTPS,
				       double peakTPS, Statistics total) {
			testGraph.add(tps, averageTPS, peakTPS, total);
		    }
		}
		);

	    add(testGraph);
	}

	model.addTotalSampleListener(
	    new SampleListener() {
		public void update(double tps, double averageTPS,
				   double peakTPS, Statistics total) {
		    LabelledGraph.resetPeak();
		}
	    });
    }
}
