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

package net.grinder.console.model;

import junit.framework.TestCase;
import junit.swingui.TestRunner;
//import junit.textui.TestRunner;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;

import net.grinder.common.GrinderException;


/**
 * @author Philip Aston
 * @version $Revision$
 */
public class TestConsoleProperties extends TestCase
{
    public static void main(String[] args)
    {
	TestRunner.run(TestConsoleProperties.class);
    }

    public TestConsoleProperties(String name)
    {
	super(name);
    }

    private File m_file;
    private FileWriter m_fileWriter;
    private Random m_random = new Random();

    protected void setUp() throws Exception
    {
	m_file = File.createTempFile("testing", "123");
	m_file.deleteOnExit();

	m_fileWriter = new FileWriter(m_file);
    }

    public void testCollectSamples() throws Exception
    {
	new TestIntTemplate(ConsoleProperties.COLLECT_SAMPLES_PROPERTY) 
	{
	    protected int get(ConsoleProperties properties)
	    {
		return properties.getCollectSampleCount();
	    }

	    protected void set(ConsoleProperties properties, int i)
	    {
		properties.setCollectSampleCount(i);
	    }
	}.doTest();
    }

    public void testIgnoreSamples() throws Exception
    {
	new TestIntTemplate(ConsoleProperties.IGNORE_SAMPLES_PROPERTY) 
	{
	    protected int get(ConsoleProperties properties)
	    {
		return properties.getIgnoreSampleCount();
	    }

	    protected void set(ConsoleProperties properties, int i)
	    {
		properties.setIgnoreSampleCount(i);
	    }
	}.doTest();
    }

    public void testSampleInterval() throws Exception
    {
	new TestIntTemplate(ConsoleProperties.SAMPLE_INTERVAL_PROPERTY) 
	{
	    protected int get(ConsoleProperties properties)
	    {
		return properties.getSampleInterval();
	    }

	    protected void set(ConsoleProperties properties, int i)
	    {
		properties.setSampleInterval(i);
	    }
	}.doTest();
    }

    public void testSignificantFigures() throws Exception
    {
	new TestIntTemplate(ConsoleProperties.SIG_FIG_PROPERTY) 
	{
	    protected int get(ConsoleProperties properties)
	    {
		return properties.getSignificantFigures();
	    }

	    protected void set(ConsoleProperties properties, int i)
	    {
		properties.setSignificantFigures(i);
	    }
	}.doTest();
    }

    public void testMulticastAddress() throws Exception
    {
	new TestStringTemplate(ConsoleProperties.MULTICAST_ADDRESS_PROPERTY) 
	{
	    protected String get(ConsoleProperties properties)
	    {
		return properties.getMulticastAddress();
	    }

	    protected void set(ConsoleProperties properties, String s)
	    {
		properties.setMulticastAddress(s);
	    }
	}.doTest();
    }

    public void testConsolePort() throws Exception
    {
	new TestIntTemplate(ConsoleProperties.CONSOLE_PORT_PROPERTY) 
	{
	    protected int get(ConsoleProperties properties)
	    {
		return properties.getConsolePort();
	    }

	    protected void set(ConsoleProperties properties, int i)
	    {
		properties.setConsolePort(i);
	    }
	}.doTest();
    }

    public void testGrinderPort() throws Exception
    {
	new TestIntTemplate(ConsoleProperties.GRINDER_PORT_PROPERTY) 
	{
	    protected int get(ConsoleProperties properties)
	    {
		return properties.getGrinderPort();
	    }

	    protected void set(ConsoleProperties properties, int i)
	    {
		properties.setGrinderPort(i);
	    }
	}.doTest();
    }

    private abstract class TestIntTemplate
    {
	private final String m_propertyName;

	public TestIntTemplate(String propertyName)
	{
	    m_propertyName = propertyName;
	}

	public void doTest() throws Exception
	{
	    final int i1 = m_random.nextInt();

	    m_fileWriter.write(m_propertyName + ":" + i1);
	    m_fileWriter.close();

	    final ConsoleProperties properties = new ConsoleProperties(m_file);
	    assertEquals(i1, get(properties));

	    final int i2 = m_random.nextInt();

	    set(properties, i2);
	    assertEquals(i2, get(properties));

	    properties.save();

	    final ConsoleProperties properties2 =
		new ConsoleProperties(m_file);
	    assertEquals(i2, get(properties2));

	    final int i3 = m_random.nextInt();

	    final PropertyChangeEvent expected =
		new PropertyChangeEvent(properties2, m_propertyName, 
					new Integer(i2), new Integer(i3));

	    final MyListener listener = new MyListener(expected);
	    final MyListener listener2 = new MyListener(expected);

	    properties2.addPropertyChangeListener(listener);
	    properties2.addPropertyChangeListener(m_propertyName, listener);

	    set(properties2, i3);
	}

	protected abstract int get(ConsoleProperties properties);
	protected abstract void set(ConsoleProperties properties, int i);

    }

    private abstract class TestStringTemplate
    {
	private final String m_propertyName;

	public TestStringTemplate(String propertyName)
	{
	    m_propertyName = propertyName;
	}

	public void doTest() throws Exception
	{
	    final String s1 = Integer.toString(m_random.nextInt());

	    m_fileWriter.write(m_propertyName + ":" + s1);
	    m_fileWriter.close();

	    final ConsoleProperties properties = new ConsoleProperties(m_file);
	    assertEquals(s1, get(properties));

	    final String s2 = Integer.toString(m_random.nextInt());

	    set(properties, s2);
	    assertEquals(s2, get(properties));

	    properties.save();

	    final ConsoleProperties properties2 =
		new ConsoleProperties(m_file);
	    assertEquals(s2, get(properties2));

	    final String s3 = Integer.toString(m_random.nextInt());

	    final PropertyChangeEvent expected =
		new PropertyChangeEvent(properties2, m_propertyName, s2, s3);

	    final MyListener listener = new MyListener(expected);
	    final MyListener listener2 = new MyListener(expected);

	    properties2.addPropertyChangeListener(listener);
	    properties2.addPropertyChangeListener(m_propertyName, listener);

	    set(properties2, s3);
	}

	protected abstract String get(ConsoleProperties properties);
	protected abstract void set(ConsoleProperties properties, String s);
    }

    private class MyListener implements PropertyChangeListener
    {
	final PropertyChangeEvent m_expected;

	MyListener(PropertyChangeEvent expected) 
	{
	    m_expected = expected;
	}

	public void propertyChange(PropertyChangeEvent event)
	{
	    assertEquals(m_expected.getOldValue(), event.getOldValue());
	    assertEquals(m_expected.getNewValue(), event.getNewValue());
	    assertEquals(m_expected.getPropertyName(),
			 event.getPropertyName());
	}
    }
}

