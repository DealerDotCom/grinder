// Copyright (C) 2001, 2002 Philip Aston
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

package net.grinder.script;

import net.grinder.common.GrinderException;
import net.grinder.common.Logger;
import net.grinder.common.Test;


/**
 * Scripts can get contextual information through a global
 * <code>grinder</code> object that supports this interface.
 *
 * @author Philip Aston
 * @version $Revision$
 */ 
public interface ScriptContext
{
    /**
     * Get an unique ID value for this Worker Process.
     *
     * @return The id.
     */
    String getGrinderID();

    /**
     * Return the thread ID, or -1 if not called from a Worker Thread.
     * @return The thread ID.
     */
    int getThreadID();

    /**
     * Return the current run number, or -1 if not called from a
     * Worker Thread.
     *
     * @return An <code>int</code> value.
     */
    int getRunNumber();

    /**
     * Get an appropriate {@link net.grinder.common.Logger}
     * implementation. The value returned when invoked from script
     * initialisation differs from the value returned when called from
     * a Worker Thread, so its best not to keep references to the
     * result.
     *
     * @return A <code>Logger</code>.
     */
    Logger getLogger();

    /**
     * Sleep for a time based on the meanTime parameter. The actual
     * time may be greater or less than meanTime, and is distributed
     * according to a pseudo normal distribution.
     *
     * @param meanTime Mean time in milliseconds.
     * @exception GrinderException If an error occurs.
     */
    void sleep(long meanTime) throws GrinderException;

    /**
     * Sleep for a time based on the meanTime parameter. The actual
     * time may be greater or less than meanTime, and is distributed
     * according to a pseudo normal distribution.
     *
     * @param meanTime Mean time in milliseconds.
     * @param sigma The standard deviation, in milliseconds.
     * @exception GrinderException If an error occurs.
     **/
    void sleep(long meanTime, long sigma) throws GrinderException;
}
