// The Grinder
// Copyright (C) 2000, 2001 Paco Gomez
// Copyright (C) 2000, 2001 Philip Aston

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

package net.grinder.console;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.communication.Message;
import net.grinder.communication.RegisterTestsMessage;
import net.grinder.communication.ReportStatisticsMessage;
import net.grinder.console.model.Model;
import net.grinder.console.swingui.ConsoleUI;



/**
 * This is the entry point of The Grinder Console.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public class Console
{
    private final ConsoleCommunication m_communication;
    private final Model m_model;
    private final ConsoleUI m_userInterface;

    public Console()
	throws GrinderException
    {
	// !!
	final GrinderProperties properties =
	    new net.grinder.util.PropertiesHelper().getProperties();

	m_communication = new ConsoleCommunication(properties);

	m_model = new Model();

	final ActionListener startHandler =
	    new ActionListener() {
		    public void actionPerformed(ActionEvent event) {
			try {
			    m_communication.sendStartMessage();
			}
			catch (GrinderException e) {
			    System.err.println(
				"Could not send start message: " + e);
			    e.printStackTrace();
			}
		    }
		};

	final ActionListener resetHandler =
	    new ActionListener() {
		    public void actionPerformed(ActionEvent event) {
			try {
			    m_communication.sendResetMessage();
			}
			catch (GrinderException e) {
			    System.err.println(
				"Could not send start message: " + e);
			    e.printStackTrace();
			}
		    }
		};

	final ActionListener stopHandler =
	    new ActionListener() {
		    public void actionPerformed(ActionEvent event) {
			try {
			    m_communication.sendStopMessage();
			}
			catch (GrinderException e) {
			    System.err.println(
				"Could not send stop message: " + e);
			    e.printStackTrace();
			}
		    }
		};

	m_userInterface = new ConsoleUI(m_model, startHandler, resetHandler,
					stopHandler);
    }
    
    public void run() throws GrinderException
    {
        while (true) {
	    final Message message = m_communication.waitForMessage();

	    if (message instanceof RegisterTestsMessage) {
		m_model.registerTests(
		    ((RegisterTestsMessage)message).getTests());
	    }
	    
	    if (message instanceof ReportStatisticsMessage) {
		m_model.add(
		    ((ReportStatisticsMessage)message).getStatisticsDelta());
	    }
        } 
    }
}
