// The Grinder
// Copyright (C) 2001  Paco Gomez
// Copyright (C) 2001  Philip Aston

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

package net.grinder.plugin.http;

import net.grinder.plugininterface.ThreadCallbacks;


/**
 * This interface can be optionally implemented by "String Bean"
 * classes used by the HTTP plugin that want to know more about the
 * test lifecycle.
 *
 * If a String Bean implements this interface, the corresponding
 * methods are called on the bean before they are called on the
 * plugin's ThreadCallbacks object.
 *
 * Not sure whether extending ThreadCallbacks is the right thing to do
 * because the "doTest" method needs to return a boolean. For now,
 * lets not worry too much about this.
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
public interface StringBean extends ThreadCallbacks
{
}
