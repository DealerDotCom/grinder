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


/**
 * Abstract base class for simple plugins that use the default test
 * set mechanism and wish to focus on implementing ThreadCallbacks.
 *
 * @author Philip Aston
 * @version $Revision$
 * @deprecated I now consider plugins that use this to be less simple
 * than those that explicitly create ThreadCallbacks objects.
 */ 
public abstract class SimplePluginBase
    implements GrinderPlugin, ThreadCallbacks
{
    private Set m_testsFromPropertiesFile;

    public void initialize(PluginProcessContext processContext,
			   Set testsFromPropertiesFile)
	throws PluginException
    {
	m_testsFromPropertiesFile = testsFromPropertiesFile;
    }

    public ThreadCallbacks createThreadCallbackHandler()
	throws PluginException
    {
	try {
	    final SimplePluginBase result =
		(SimplePluginBase)getClass().newInstance();

	    result.m_testsFromPropertiesFile = m_testsFromPropertiesFile;
	    return result;
	}
	catch (Exception e) {
	    throw new PluginException(
		"Could not create new instance of plugin class " +
		getClass().getName(), e);
	}
    }

    public Set getTests() throws PluginException
    {
	return m_testsFromPropertiesFile;
    }
}
