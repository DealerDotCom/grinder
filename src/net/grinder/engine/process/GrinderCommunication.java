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

package net.grinder.engine.process;

import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.communication.Receiver;
import net.grinder.communication.Sender;
import net.grinder.communication.StartGrinderMessage;
import net.grinder.util.GrinderException;
import net.grinder.util.GrinderProperties;


/**
 * @author Philip Aston
 * @version $Revision$
 */
class GrinderCommunication
{
    final Receiver m_receiver;
    final Sender m_sender;

    GrinderCommunication(GrinderProperties properties)
	throws GrinderException
    {
	final String multicastAddress = 
	    properties.getMandatoryProperty("grinder.multicastAddress");

	final int consolePort =
	    properties.getMandatoryInt("grinder.console.multicastPort");

	final int grinderPort =
	    properties.getMandatoryInt("grinder.multicastPort");
	
	m_receiver = new Receiver(multicastAddress, grinderPort);
	m_sender = new Sender(multicastAddress, consolePort);
    }

    void waitForStartMessage()
	throws CommunicationException
    {
	while (true) {
	    final Message message = m_receiver.waitForMessage();

	    if (message instanceof StartGrinderMessage) {
		break;
	    }
	}
    }
}
