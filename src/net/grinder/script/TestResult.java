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

import net.grinder.common.Test;


/**
 * Represents the results of a test invocation.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see InvokeableTest
 */ 
public interface TestResult
{
    /**
     * Returns whether the test invocation was successful.
     *
     * @return a <code>boolean</code> value
     */
    boolean isSuccessful();

    /**
     * {@link net.grinder.plugininterface.PluginException}s reported
     * by the test implementation can be retrieved with this method.
     * If no exception occurred, this will return <code>null</code>.
     *
     * @return an <code>Exception</code> value
     */
    Exception getException();
}
