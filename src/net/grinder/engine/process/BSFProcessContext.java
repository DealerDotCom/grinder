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

package net.grinder.engine.process;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.bsf.BSFException;
import com.ibm.bsf.BSFManager;

import net.grinder.common.AbstractTestSemantics;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.Test;
import net.grinder.script.InvokeableTest;
import net.grinder.script.ScriptContext;
import net.grinder.script.TestResult;
import net.grinder.util.Sleeper;


/**
 * Wrap up the context information necessary to invoke a BSF script.
 *
 * Package scope.
 * 
 * @author Philip Aston
 * @version $Revision$
 */
class BSFFacade
{
    private final ThreadContext m_threadContext;
    private final String m_script;
    private final String m_language;
    private final BSFManager m_bsfManager;

    public static String loadScript(File scriptFile)
	throws GrinderException
    {
	try {
	    final char[] data = new char[(int)scriptFile.length()];

	    final FileReader reader = new FileReader(scriptFile);
	    reader.read(data);
	    reader.close();

	    return new String(data);
	}
	catch (IOException e) {
	    throw new GrinderException("Could not read script file", e);
	}
    }

    public static String getScriptLanguage(File scriptFile)
	throws GrinderException
    {
	try {
	    final String language =
		BSFManager.getLangFromFilename(scriptFile.getPath());
	    new BSFManager().loadScriptingEngine(language);

	    return language;
	}
	catch (BSFException e) {
	    throw new GrinderException("BSF exception", e);
	}
    }

    public BSFFacade(ThreadContext threadContext, String script,
		     String language)
	throws GrinderException
    {
	m_threadContext = threadContext;
	m_script = script;
	m_language = language;
	m_bsfManager = new BSFManager();

	try {
	    m_bsfManager.declareBean("context", new BSFScriptContext(),
				     ScriptContext.class);
	}
	catch (BSFException e) {
	    throw new GrinderException("BSF exception", e);
	}
    }

    public void run() throws GrinderException
    {
	try {
	    m_bsfManager.exec(m_language, "Grinder", 0, 0, m_script);
	}
	catch (BSFException e) {
	    throw new GrinderException("Exception whilst invoking script", e);
	}
    }

    private class BSFScriptContext implements ScriptContext
    {
	private final Test[] m_tests;

	public BSFScriptContext() 
	{
	    final List testDataList =  m_threadContext.getTests();
	    m_tests = new Test[testDataList.size()];

	    final Iterator iterator = testDataList.iterator();
	    int i = 0;
	    
	    while (iterator.hasNext()) {
		final TestData testData = (TestData)iterator.next();
		m_tests[i++] = new BSFInvokeableTest(testData);
	    }
	}

	public Logger getLogger()
	{
	    return m_threadContext;
	}

	public Test[] getTests()
	{
	    return m_tests;
	}

	public String getGrinderID()
	{
	    return m_threadContext.getGrinderID();
	}

	public int getThreadID()
	{
	    return m_threadContext.getThreadID();
	}
    }

    private class BSFInvokeableTest
	extends AbstractTestSemantics implements InvokeableTest
    {
	private final TestData m_testData;

	BSFInvokeableTest(TestData testData)
	{
	    m_testData = testData;
	}

	public final int getNumber()
	{
	    return m_testData.getTest().getNumber();
	}

	public final String getDescription()
	{
	    return m_testData.getTest().getDescription();
	}

	public final GrinderProperties getParameters()
	{
	    return m_testData.getTest().getParameters();
	}

	public TestResult invoke()
	{
	    try {
		m_threadContext.invokeTest(m_testData);
	    }
	    catch (Sleeper.ShutdownException e) {
	    }
	    
	    return null;
	}
    }
}
