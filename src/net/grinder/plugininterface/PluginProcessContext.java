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

package net.grinder.plugininterface;

import java.util.Set;

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.statistics.StatisticsView;


/**
 * <p>This class is used to share process information between the
 * Grinder and the plug-in.</p>
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 **/
public interface PluginProcessContext extends Logger, FilenameFactory
{
    /**
     * Returns the name of this Grinder Process.
     *
     * @return The name.
     **/
    String getGrinderID();

    /**
     * Plugins can use this method to register a new "summary"
     * statistics view. These views appear in the worker process
     * summary table and the console.
     *
     * @param view The new view.
     * @exception GrinderException If the view cannot be registered.
     **/
    void registerSummaryStatisticsView(StatisticsView view)
	throws GrinderException;

    /**
     * Plugins can use this method to register a new "detail"
     * statistics view. These views appear in the individual process
     * data files.
     *
     * @param view The new view.
     * @exception GrinderException If the view cannot be registered.
     **/
    void registerDetailStatisticsView(StatisticsView view)
	throws GrinderException;

    /**
     * Check whether this process is reporting times to the console or
     * not. Refer to the <code>grinder.recordTime</code> property for
     * more information.
     *
     * @return <code>true => this process should report times.
     **/
    boolean getRecordTime();

    /**
     * Return the {@link PluginThreadCallbacks} for the current
     * thread.
     */
    PluginThreadCallbacks getPluginThreadCallbacks()
	throws GrinderException;
}
