// Copyright (C) 2000, 2001, 2002, 2003, 2004 Philip Aston
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

package net.grinder.console.model;

import junit.framework.TestCase;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Random;

import net.grinder.communication.CommunicationDefaults;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.testutility.AssertUtilities;


/**
 * Unit test for {@link ConsoleProperties}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestConsoleProperties extends TestCase {

  private static final Resources s_resources =
      new Resources("net.grinder.console.swingui.resources.Console");

  private File m_file;
  private Random m_random = new Random();

  protected void setUp() throws Exception {
    m_file = File.createTempFile("testing", "123");
    m_file.deleteOnExit();
  }

  public void testCollectSamples() throws Exception {

    new TestIntTemplate(ConsoleProperties.COLLECT_SAMPLES_PROPERTY, 0,
			Integer.MAX_VALUE) {

      protected int get(ConsoleProperties properties) {
	return properties.getCollectSampleCount();
      }

      protected void set(ConsoleProperties properties, int i)
	throws DisplayMessageConsoleException {
	properties.setCollectSampleCount(i);
      }
    }.doTest();
  }

  public void testIgnoreSamples() throws Exception {

    new TestIntTemplate(ConsoleProperties.IGNORE_SAMPLES_PROPERTY, 0,
			Integer.MAX_VALUE) {

      protected int get(ConsoleProperties properties) {
	return properties.getIgnoreSampleCount();
      }

      protected void set(ConsoleProperties properties, int i)
	throws DisplayMessageConsoleException {
	properties.setIgnoreSampleCount(i);
      }
    }.doTest();
  }

  public void testSampleInterval() throws Exception {

    new TestIntTemplate(ConsoleProperties.SAMPLE_INTERVAL_PROPERTY, 1,
			Integer.MAX_VALUE) {

      protected int get(ConsoleProperties properties) {
	return properties.getSampleInterval();
      }

      protected void set(ConsoleProperties properties, int i)
	throws DisplayMessageConsoleException {
	properties.setSampleInterval(i);
      }
    }.doTest();
  }

  public void testSignificantFigures() throws Exception {

    new TestIntTemplate(ConsoleProperties.SIG_FIG_PROPERTY, 0,
			Integer.MAX_VALUE) {

      protected int get(ConsoleProperties properties) {
	return properties.getSignificantFigures();
      }

      protected void set(ConsoleProperties properties, int i)
	throws DisplayMessageConsoleException {
	properties.setSignificantFigures(i);
      }
    }.doTest();
  }

  public void testConsoleHost() throws Exception {

    final String propertyName = ConsoleProperties.CONSOLE_HOST_PROPERTY;

    final String s1 = "123.1.2.3";

    writePropertyToFile(propertyName, s1);

    final ConsoleProperties properties =
      new ConsoleProperties(s_resources, m_file);

    assertEquals(s1, properties.getConsoleHost());

    final String s2 = "123.99.33.11";

    properties.setConsoleHost(s2);
    assertEquals(s2, properties.getConsoleHost());

    properties.save();

    final ConsoleProperties properties2 =
      new ConsoleProperties(s_resources, m_file);

    assertEquals(s2, properties2.getConsoleHost());

    final String s3 = "1.46.68.80";

    final PropertyChangeEvent expected =
      new PropertyChangeEvent(properties2, propertyName, s2, s3);

    final MyListener listener = new MyListener(expected);
    final MyListener listener2 = new MyListener(expected);

    properties2.addPropertyChangeListener(listener);
    properties2.addPropertyChangeListener(propertyName, listener2);

    properties2.setConsoleHost(s3);

    listener.assertCalledOnce();
    listener2.assertCalledOnce();

    try {
      properties.setConsoleHost("234.12.23.2");
      fail("Expected a DisplayMessageConsoleException for multicast address");
    }
    catch (DisplayMessageConsoleException e) {
    }
  }

  public void testConsolePort() throws Exception {

    new TestIntTemplate(ConsoleProperties.CONSOLE_PORT_PROPERTY,
                        CommunicationDefaults.MIN_PORT,
			CommunicationDefaults.MAX_PORT) {

      protected int get(ConsoleProperties properties) {
	return properties.getConsolePort();
      }

      protected void set(ConsoleProperties properties, int i)
	throws DisplayMessageConsoleException {
	properties.setConsolePort(i);
      }
    }.doTest();
  }

  public void testResetConsoleWithProcesses() throws Exception {
    new TestBooleanTemplate(
      ConsoleProperties.RESET_CONSOLE_WITH_PROCESSES_PROPERTY) {

      protected boolean get(ConsoleProperties properties) {
	return properties.getResetConsoleWithProcesses();
      }

      protected void set(ConsoleProperties properties, boolean b) {
	properties.setResetConsoleWithProcesses(b);
      }
      
    }.doTest();
  }

  public void testResetConsoleWithProcessesDontAsk() throws Exception {

    final String propertyName =
      ConsoleProperties.RESET_CONSOLE_WITH_PROCESSES_DONT_ASK_PROPERTY;

    writePropertyToFile(propertyName, "false");

    final ConsoleProperties properties =
      new ConsoleProperties(s_resources, m_file);

    assertTrue(!properties.getResetConsoleWithProcessesDontAsk());

    final PropertyChangeEvent expected =
      new PropertyChangeEvent(properties, propertyName, 
			      new Boolean(false), new Boolean(true));

    final MyListener listener = new MyListener(expected);
    final MyListener listener2 = new MyListener(expected);

    properties.addPropertyChangeListener(listener);
    properties.addPropertyChangeListener(propertyName, listener2);

    properties.setResetConsoleWithProcessesDontAsk();
    
    final ConsoleProperties properties2 =
      new ConsoleProperties(s_resources, m_file);

    assertTrue(properties2.getResetConsoleWithProcessesDontAsk());

    listener.assertCalledOnce();
    listener2.assertCalledOnce();
  }

  public void testStopProcessesDontAsk() throws Exception {

    final String propertyName =
      ConsoleProperties.STOP_PROCESSES_DONT_ASK_PROPERTY;

    writePropertyToFile(propertyName, "false");

    final ConsoleProperties properties =
      new ConsoleProperties(s_resources, m_file);

    assertTrue(!properties.getStopProcessesDontAsk());

    final PropertyChangeEvent expected =
      new PropertyChangeEvent(properties, propertyName, 
			      new Boolean(false), new Boolean(true));

    final MyListener listener = new MyListener(expected);
    final MyListener listener2 = new MyListener(expected);

    properties.addPropertyChangeListener(listener);
    properties.addPropertyChangeListener(propertyName, listener2);

    properties.setStopProcessesDontAsk();
    
    final ConsoleProperties properties2 =
      new ConsoleProperties(s_resources, m_file);

    assertTrue(properties2.getStopProcessesDontAsk());

    listener.assertCalledOnce();
    listener2.assertCalledOnce();
  }

  public void testScriptFile() throws Exception {

    new TestFileTemplate(ConsoleProperties.SCRIPT_FILE_PROPERTY) {

      protected File get(ConsoleProperties properties) {
	return properties.getScriptFile();
      }

      protected void set(ConsoleProperties properties, File file) {
	properties.setScriptFile(file);
      }
    }.doTest();
  }

  public void testDistributionDirectory() throws Exception {

    new TestFileTemplate(ConsoleProperties.DISTRIBUTION_DIRECTORY_PROPERTY) {

      protected File get(ConsoleProperties properties) {
	return properties.getDistributionDirectory();
      }

      protected void set(ConsoleProperties properties, File file) {
	properties.setDistributionDirectory(file);
      }
    }.doTest();

    // Check default is not null.
    final File file = File.createTempFile("testing", "123");
    file.deleteOnExit();

    final ConsoleProperties properties =
      new ConsoleProperties(s_resources, file);

    properties.setDistributionDirectory(null);
    assertNotNull(properties.getDistributionDirectory());

    properties.saveDistributionDirectory();

    final Properties rawProperties = new Properties();
    rawProperties.load(new FileInputStream(file));
    assertEquals(1, rawProperties.size());
    assertEquals(rawProperties.getProperty(
                   ConsoleProperties.DISTRIBUTION_DIRECTORY_PROPERTY),
                 properties.getDistributionDirectory().getPath());
  }

  public void testLookAndFeel() throws Exception {

    new TestStringTemplate(ConsoleProperties.LOOK_AND_FEEL_PROPERTY, true) {

      protected String get(ConsoleProperties properties) {
	return properties.getLookAndFeel();
      }

      protected void set(ConsoleProperties properties, String name) {
	properties.setLookAndFeel(name);
      }
    }.doTest();
  }

  public void testCopyConstructor() throws Exception {
    final ConsoleProperties p1 = new ConsoleProperties(s_resources, m_file);
    final ConsoleProperties p2 = new ConsoleProperties(p1);

    assertEquals(p1.getCollectSampleCount(), p2.getCollectSampleCount());
    assertEquals(p1.getIgnoreSampleCount(), p2.getIgnoreSampleCount());
    assertEquals(p1.getSampleInterval(), p2.getSampleInterval());
    assertEquals(p1.getSignificantFigures(), p2.getSignificantFigures());
    assertEquals(p1.getConsoleHost(), p2.getConsoleHost());
    assertEquals(p1.getConsolePort(), p2.getConsolePort());
    assertEquals(p1.getResetConsoleWithProcesses(),
		 p2.getResetConsoleWithProcesses());
    assertEquals(p1.getResetConsoleWithProcessesDontAsk(),
		 p2.getResetConsoleWithProcessesDontAsk());
    assertEquals(p1.getStopProcessesDontAsk(), p2.getStopProcessesDontAsk());
    assertEquals(p1.getScriptFile(), p2.getScriptFile());
    assertEquals(p1.getDistributionDirectory(), p2.getDistributionDirectory());
    assertEquals(p1.getLookAndFeel(), p2.getLookAndFeel());
  }

  public void testAssignment() throws Exception {
    final ConsoleProperties p1 = new ConsoleProperties(s_resources, m_file);
    final ConsoleProperties p2 = new ConsoleProperties(s_resources, m_file);
    p2.setCollectSampleCount(99);
    p2.setIgnoreSampleCount(99);
    p2.setSampleInterval(99);
    p2.setSignificantFigures(99);
    p2.setConsoleHost("99.99.99.99");
    p2.setConsolePort(99);
    p2.setResetConsoleWithProcesses(true);
    p2.setResetConsoleWithProcessesDontAsk();
    p2.setStopProcessesDontAsk();
    p2.setScriptFile(new File("foo"));
    p2.setDistributionDirectory(new File("bah"));
    p2.setLookAndFeel("something");

    assertTrue(p1.getCollectSampleCount() != p2.getCollectSampleCount());
    assertTrue(p1.getIgnoreSampleCount() != p2.getIgnoreSampleCount());
    assertTrue(p1.getSampleInterval() != p2.getSampleInterval());
    assertTrue(p1.getSignificantFigures() != p2.getSignificantFigures());
    assertTrue(!p1.getConsoleHost().equals(p2.getConsoleHost()));
    assertTrue(p1.getConsolePort() != p2.getConsolePort());
    assertTrue(p1.getResetConsoleWithProcesses() !=
	       p2.getResetConsoleWithProcesses());
    assertTrue(p1.getResetConsoleWithProcessesDontAsk() !=
	       p2.getResetConsoleWithProcessesDontAsk());
    assertTrue(p1.getStopProcessesDontAsk() != p2.getStopProcessesDontAsk());
    AssertUtilities.assertNotEquals(p1.getScriptFile(), p2.getScriptFile());
    AssertUtilities.assertNotEquals(p1.getDistributionDirectory(),
                                    p2.getDistributionDirectory());
    AssertUtilities.assertNotEquals(p1.getLookAndFeel(), p2.getLookAndFeel());

    p2.set(p1);

    assertEquals(p1.getCollectSampleCount(), p2.getCollectSampleCount());
    assertEquals(p1.getIgnoreSampleCount(), p2.getIgnoreSampleCount());
    assertEquals(p1.getSampleInterval(), p2.getSampleInterval());
    assertEquals(p1.getSignificantFigures(), p2.getSignificantFigures());
    assertEquals(p1.getConsoleHost(), p2.getConsoleHost());
    assertEquals(p1.getConsolePort(), p2.getConsolePort());
    assertTrue(p1.getResetConsoleWithProcesses() ==
	       p2.getResetConsoleWithProcesses());
    assertTrue(p1.getResetConsoleWithProcessesDontAsk() ==
	       p2.getResetConsoleWithProcessesDontAsk());
    assertTrue(p1.getStopProcessesDontAsk() == p2.getStopProcessesDontAsk());
    assertEquals(p1.getScriptFile(), p2.getScriptFile());
    assertEquals(p1.getDistributionDirectory(), p2.getDistributionDirectory());
    assertEquals(p1.getLookAndFeel(), p2.getLookAndFeel());
  }

  private abstract class TestIntTemplate {
    private final String m_propertyName;
    private final int m_minimum;
    private final int m_maximum;

    public TestIntTemplate(String propertyName, int minimum, int maximum) {
      if (maximum <= minimum) {
	throw new IllegalArgumentException(
	  "Minimum not less than maximum");
      }

      m_propertyName = propertyName;
      m_minimum = minimum;
      m_maximum = maximum;
    }

    private int getRandomInt() {
      return getRandomInt(m_minimum, m_maximum);
    }

    private int getRandomInt(int minimum, int maximum) {
      // Valid values are in [minimum, maximum], so range is 1
      // more than maximum value.Will not fit in an int, use a
      // long.
      long range = (long)maximum + 1 - minimum;

      return (int)(minimum + Math.abs(m_random.nextLong()) % range);
    }

    public void doTest() throws Exception {
      final int i1 = getRandomInt();

      writePropertyToFile(m_propertyName, Integer.toString(i1));

      final ConsoleProperties properties =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(i1, get(properties));

      final int i2 = getRandomInt();

      set(properties, i2);
      assertEquals(i2, get(properties));

      properties.save();

      final ConsoleProperties properties2 =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(i2, get(properties2));

      int i3;

      do {
        i3 = getRandomInt();
      }
      while (i3 == i2);

      final PropertyChangeEvent expected =
	new PropertyChangeEvent(properties2, m_propertyName, 
				new Integer(i2), new Integer(i3));

      final MyListener listener = new MyListener(expected);
      final MyListener listener2 = new MyListener(expected);

      properties2.addPropertyChangeListener(listener);
      properties2.addPropertyChangeListener(m_propertyName, listener2);

      set(properties2, i3);

      if (m_minimum > Integer.MIN_VALUE) {
	try {
	  set(properties, m_minimum - 1);
	  fail("Should not reach");
	}
	catch (DisplayMessageConsoleException e) {
	}

	try {
	  set(properties, Integer.MIN_VALUE);
	  fail("Should not reach");
	}
	catch (DisplayMessageConsoleException e) {
	}

	try {
	  set(properties, getRandomInt(Integer.MIN_VALUE,
				       m_minimum - 1));
	  fail("Should not reach");
	}
	catch (DisplayMessageConsoleException e) {
	}
      }

      if (m_maximum < Integer.MAX_VALUE) {
	try {
	  set(properties, m_maximum + 1);
	  fail("Should not reach");
	}
	catch (DisplayMessageConsoleException e) {
	}

	try {
	  set(properties, Integer.MAX_VALUE);
	  fail("Should not reach");
	}
	catch (DisplayMessageConsoleException e) {
	}

	try {
	  set(properties, getRandomInt(m_maximum + 1,
				       Integer.MAX_VALUE));
	  fail("Should not reach");
	}
	catch (DisplayMessageConsoleException e) {
	}
      }

      listener.assertCalled();
      listener2.assertCalled();
    }

    protected abstract int get(ConsoleProperties properties);

    protected abstract void set(ConsoleProperties properties, int i)
      throws DisplayMessageConsoleException;
  }

  private abstract class TestBooleanTemplate {
    private final String m_propertyName;

    public TestBooleanTemplate(String propertyName) {
      m_propertyName = propertyName;
    }

    public void doTest() throws Exception {

      writePropertyToFile(m_propertyName, "false");

      final ConsoleProperties properties =
        new ConsoleProperties(s_resources, m_file);

      assertTrue(!get(properties));

      set(properties, true);
      assertTrue(get(properties));

      properties.save();

      final ConsoleProperties properties2 =
        new ConsoleProperties(s_resources, m_file);

      assertTrue(get(properties2));

      final PropertyChangeEvent expected =
	new PropertyChangeEvent(properties2, m_propertyName, 
				new Boolean(true), new Boolean(false));

      final MyListener listener = new MyListener(expected);
      final MyListener listener2 = new MyListener(expected);

      properties2.addPropertyChangeListener(listener);
      properties2.addPropertyChangeListener(m_propertyName, listener2);

      set(properties2, false);

      listener.assertCalledOnce();
      listener2.assertCalledOnce();
    }

    protected abstract boolean get(ConsoleProperties properties);

    protected abstract void set(ConsoleProperties properties, boolean b);
  }

  private String getRandomString() {
      final int length = m_random.nextInt(200);
      final char[] characters = new char[length];

      for (int i=0; i<characters.length; ++i) {
        characters[i] = (char)(0x20 + m_random.nextInt(0x60));
      }

      return new String(characters);
  }

  private abstract class TestStringTemplate {
    private final String m_propertyName;
    private final boolean m_allowNulls;

    public TestStringTemplate(String propertyName, boolean allowNulls) {
      m_propertyName = propertyName;
      m_allowNulls = allowNulls;
    }

    public void doTest() throws Exception {

      if (m_allowNulls) {
        final ConsoleProperties properties =
          new ConsoleProperties(s_resources, m_file);

        assertNull(get(properties));

        final String s = getRandomString();
        set(properties, s);
        assertNotNull(get(properties));

        set(properties, null);
        assertNull(get(properties));

        properties.save();

        final ConsoleProperties properties2 =
          new ConsoleProperties(s_resources, m_file);

        assertNull(get(properties2));
      }
      else {
        final ConsoleProperties properties =
          new ConsoleProperties(s_resources, m_file);

        try {
          set(properties, null);
          fail("Can set '" + m_propertyName +
               "' to null, expected DisplayMessageConsoleException");
        }
        catch (DisplayMessageConsoleException e) {
        }
      }
      
      final String s1 = getRandomString();

      writePropertyToFile(m_propertyName, s1);

      final ConsoleProperties properties =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(s1, get(properties));

      final String s2 = getRandomString();

      set(properties, s2);
      assertEquals(s2, get(properties));

      properties.save();

      final ConsoleProperties properties2 =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(s2, get(properties2));

      String s3 = getRandomString();

      do {
        s3 = getRandomString();
      }
      while (s3.equals(s2));

      final PropertyChangeEvent expected =
	new PropertyChangeEvent(properties2, m_propertyName, s2, s3);

      final MyListener listener = new MyListener(expected);
      final MyListener listener2 = new MyListener(expected);

      properties2.addPropertyChangeListener(listener);
      properties2.addPropertyChangeListener(m_propertyName, listener2);

      set(properties2, s3);

      listener.assertCalledOnce();
      listener2.assertCalledOnce();
    }

    protected abstract String get(ConsoleProperties properties);

    protected abstract void set(ConsoleProperties properties, String i)
      throws DisplayMessageConsoleException;
  }

  private abstract class TestFileTemplate {

    private String m_propertyName;

    public TestFileTemplate(String propertyName) {
      m_propertyName = propertyName;
    }

    private File getRandomFile() {
      return new File(getRandomString());
    }

    public void doTest() throws Exception {

      final File f1 = getRandomFile();

      writePropertyToFile(m_propertyName, f1.getPath());

      final ConsoleProperties properties =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(f1, get(properties));

      final File f2 = getRandomFile();

      set(properties, f2);
      assertEquals(f2, get(properties));

      properties.save();

      final ConsoleProperties properties2 =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(f2, get(properties2));

      File f3 = getRandomFile();

      do {
        f3 = getRandomFile();
      }
      while (f3.equals(f2));

      final PropertyChangeEvent expected =
	new PropertyChangeEvent(properties2, m_propertyName, f2, f3);

      final MyListener listener = new MyListener(expected);
      final MyListener listener2 = new MyListener(expected);

      properties2.addPropertyChangeListener(listener);
      properties2.addPropertyChangeListener(m_propertyName, listener2);

      set(properties2, f3);

      listener.assertCalledOnce();
      listener2.assertCalledOnce();
    }

    protected abstract File get(ConsoleProperties properties);

    protected abstract void set(ConsoleProperties properties, File i)
      throws DisplayMessageConsoleException;
  }

  private static final class MyListener implements PropertyChangeListener {
    private final PropertyChangeEvent m_expected;
    private int m_callCount;

    MyListener(PropertyChangeEvent expected) {
      m_expected = expected;
    }

    public void propertyChange(PropertyChangeEvent event) {
      ++m_callCount;
      assertEquals(m_expected.getOldValue(), event.getOldValue());
      assertEquals(m_expected.getNewValue(), event.getNewValue());
      assertEquals(m_expected.getPropertyName(), event.getPropertyName());
    }

    public void assertCalledOnce() {
      assertEquals(1, m_callCount);
    }

    public void assertCalled() {
      assertTrue(m_callCount > 0);
    }
  }

  /**
   * Write a property key/value pair to our temporary file. Use
   * Properties so we get the correct escaping.
   */
  private final void writePropertyToFile(String name, String value)
    throws Exception {

    final FileOutputStream outputStream = new FileOutputStream(m_file);

    final Properties properties = new Properties();
    properties.setProperty(name, value);
    properties.store(outputStream, "");
    outputStream.close();
  }
}

