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

package net.grinder.communication;


/**
 * Class that manages the sending of multicast messages.
 *
 * @author Philip Aston
 * @version $Revision$
 **/
public interface Sender
{
    /**
     * First flush any pending messages queued with {@link #queue} and
     * then send the given message.
     *
     * @param message A {@link Message}.
     * @exception CommunicationException If an error occurs.
     **/
    void send(Message message) throws CommunicationException;

    /**
     * Flush any pending messages queued with {@link #queue}.
     *
     * @exception CommunicationException If an error occurs.
     **/
    void flush() throws CommunicationException;

    /**
     * Queue the given message for later sending.
     *
     * @param message A {@link Message}.
     * @exception CommunicationException If an error occurs.
     * @see #flush
     * @see #send
     **/
    void queue(Message message) throws CommunicationException;

    /**
     * Cleanly shutdown the <code>Sender</code>.
     *
     * @exception CommunicationException If an error occurs.
     **/
    void shutdown() throws CommunicationException;
}
