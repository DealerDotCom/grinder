// Copyright (C) 2001, 2002, 2003 Philip Aston
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.console.common.DisplayMessageConsoleException;


/**
 * Class encapsulating the console options.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ConsoleProperties {

  /** Property name **/
  public static final String COLLECT_SAMPLES_PROPERTY = 
    "grinder.console.numberToCollect";

  /** Property name **/
  public static final String IGNORE_SAMPLES_PROPERTY =
    "grinder.console.numberToIgnore";

  /** Property name **/
  public static final String SAMPLE_INTERVAL_PROPERTY =
    "grinder.console.sampleInterval";

  /** Property name **/
  public static final String SIG_FIG_PROPERTY =
    "grinder.console.significantFigures";

  /** Property name **/
  public static final String CONSOLE_ADDRESS_PROPERTY =
    "grinder.console.consoleAddress";

  /** Property name **/
  public static final String CONSOLE_PORT_PROPERTY = 
    "grinder.console.consolePort";

  /** Property name **/
  public static final String GRINDER_ADDRESS_PROPERTY =
    "grinder.console.grinderAddress";

  /** Property name **/
  public static final String GRINDER_PORT_PROPERTY =
    "grinder.console.grinderPort";

  /** Property name **/
  public static final String RESET_CONSOLE_WITH_PROCESSES_PROPERTY =
    "grinder.console.resetConsoleWithProcesses";

  /** Property name **/
  public static final String RESET_CONSOLE_WITH_PROCESSES_DONT_ASK_PROPERTY =
    "grinder.console.resetConsoleWithProcessesDontAsk";

  /** Property name **/
  public static final String SCRIPT_DISTRIBUTION_FILES_PROPERTY =
    "grinder.console.scriptDistribution.";


  private final PropertyChangeSupport m_changeSupport =
    new PropertyChangeSupport(this);

  private int m_collectSampleCount;
  private int m_ignoreSampleCount;
  private int m_sampleInterval;
  private int m_significantFigures;
  private boolean m_resetConsoleWithProcesses;
  private boolean m_resetConsoleWithProcessesDontAsk;
  private ScriptDistributionFiles m_scriptDistributionFiles;

  /**
   *We hang onto the addresses as strings so we can copy and
   *externalise them reasonably.
   **/
  private String m_consoleAddressString;
  private int m_consolePort;
  private String m_grinderAddressString;
  private int m_grinderPort;

  /**
   * Use to save and load properties, and to keep track of the
   * associated file.
   **/
  private final GrinderProperties m_properties;;

  /**
   * Construct a ConsoleProperties backed by the given file.
   * @param file The properties file.
   *
   * @exception GrinderException If the properties file cannot be
   * read. In particular a {@link
   * net.grinder.console.common.DisplayMessageConsoleException} If the
   * properties file contains invalid data.
   *
   */
  public ConsoleProperties(File file) throws GrinderException {

    m_properties = new GrinderProperties(file);

    setCollectSampleCount(
      m_properties.getInt(COLLECT_SAMPLES_PROPERTY, 0));
    setIgnoreSampleCount(m_properties.getInt(IGNORE_SAMPLES_PROPERTY, 1));
    setSampleInterval(m_properties.getInt(SAMPLE_INTERVAL_PROPERTY, 1000));
    setSignificantFigures(m_properties.getInt(SIG_FIG_PROPERTY, 3));

    setConsoleAddress(
      m_properties.getProperty(CONSOLE_ADDRESS_PROPERTY,
			       CommunicationDefaults.CONSOLE_ADDRESS));

    setConsolePort(
      m_properties.getInt(CONSOLE_PORT_PROPERTY,
			  CommunicationDefaults.CONSOLE_PORT));

    setGrinderAddress(
      m_properties.getProperty(GRINDER_ADDRESS_PROPERTY,
			       CommunicationDefaults.GRINDER_ADDRESS));

    setGrinderPort(
      m_properties.getInt(GRINDER_PORT_PROPERTY,
			  CommunicationDefaults.GRINDER_PORT));

    setResetConsoleWithProcesses(
      m_properties.getBoolean(RESET_CONSOLE_WITH_PROCESSES_PROPERTY, false));

    setResetConsoleWithProcessesDontAskInternal(
      m_properties.getBoolean(RESET_CONSOLE_WITH_PROCESSES_DONT_ASK_PROPERTY,
			      false));

    setScriptDistributionFiles(
      new ScriptDistributionFiles(SCRIPT_DISTRIBUTION_FILES_PROPERTY,
				  m_properties));
  }

  /**
   * Copy constructor. Does not copy property change listeners.
   *
   * @param properties The properties to copy.
   **/
  public ConsoleProperties(ConsoleProperties properties) {
    m_properties = properties.m_properties;
    set(properties);
  }

  /**
   * Assignment. Does not copy property change listeners, nor the
   * associated file.
   *
   * @param properties The properties to copy.
   **/
  public final void set(ConsoleProperties properties) {
    setCollectSampleCountInternal(properties.m_collectSampleCount);
    setIgnoreSampleCountInternal(properties.m_ignoreSampleCount);
    setSampleIntervalInternal(properties.m_sampleInterval);
    setSignificantFiguresInternal(properties.m_significantFigures);
    setConsoleAddressInternal(properties.m_consoleAddressString);
    setConsolePortInternal(properties.m_consolePort);
    setGrinderAddressInternal(properties.m_grinderAddressString);
    setGrinderPortInternal(properties.m_grinderPort);
    setResetConsoleWithProcesses(properties.m_resetConsoleWithProcesses);
    setResetConsoleWithProcessesDontAskInternal(
      properties.m_resetConsoleWithProcessesDontAsk);
    setScriptDistributionFiles(properties.m_scriptDistributionFiles);
  }

  /**
   * Add a <code>PropertyChangeListener</code>.
   *
   * @param listener The listener.
   **/
  public final void addPropertyChangeListener(
    PropertyChangeListener listener) {

    m_changeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Add a <code>PropertyChangeListener</code> which listens to a
   * particular property.
   *
   * @param property The property.
   * @param listener The listener.
   **/
  public final void addPropertyChangeListener(
    String property, PropertyChangeListener listener) {
    m_changeSupport.addPropertyChangeListener(property, listener);
  }

  /**
   * Save to the associated file.
   *
   * @exception GrinderException if an error occurs
   */
  public final void save() throws GrinderException {
    m_properties.setInt(COLLECT_SAMPLES_PROPERTY, m_collectSampleCount);
    m_properties.setInt(IGNORE_SAMPLES_PROPERTY, m_ignoreSampleCount);
    m_properties.setInt(SAMPLE_INTERVAL_PROPERTY, m_sampleInterval);
    m_properties.setInt(SIG_FIG_PROPERTY, m_significantFigures);
    m_properties.setProperty(CONSOLE_ADDRESS_PROPERTY,
			     m_consoleAddressString);
    m_properties.setInt(CONSOLE_PORT_PROPERTY, m_consolePort);
    m_properties.setProperty(GRINDER_ADDRESS_PROPERTY,
			     m_grinderAddressString);
    m_properties.setInt(GRINDER_PORT_PROPERTY, m_grinderPort);
    m_properties.setBoolean(RESET_CONSOLE_WITH_PROCESSES_PROPERTY,
			    m_resetConsoleWithProcesses);
    m_properties.setBoolean(RESET_CONSOLE_WITH_PROCESSES_DONT_ASK_PROPERTY,
			    m_resetConsoleWithProcessesDontAsk);

    m_scriptDistributionFiles.addToProperties(m_properties);

    m_properties.save();
  }

  /**
   * Get the number of samples to collect.
   *
   * @return The number.
   */
  public final int getCollectSampleCount() {
    return m_collectSampleCount;
  }

  /**
   * Set the number of samples to collect.
   *
   * @param n The number. 0 => forever.
   * @throws DisplayMessageConsoleException If the number is negative.
   **/
  public final void setCollectSampleCount(int n)
    throws DisplayMessageConsoleException {
    if (n < 0) {
      throw new DisplayMessageConsoleException(
	"collectNegativeError.text",
	"You must collect at least one sample, " +
	"zero means \"forever\"");
    }

    setCollectSampleCountInternal(n);
  }

  private final void setCollectSampleCountInternal(int n) {
    if (n != m_collectSampleCount) {
      final int old = m_collectSampleCount;
      m_collectSampleCount = n;
      m_changeSupport.firePropertyChange(COLLECT_SAMPLES_PROPERTY,
					 old, m_collectSampleCount);
    }
  }

  /**
   * Get the number of samples to ignore.
   *
   * @return The number.
   **/
  public final int getIgnoreSampleCount() {
    return m_ignoreSampleCount;
  }

  /**
   * Set the number of samples to collect.
   *
   * @param n The number. Must be at least 1.
   * @throws DisplayMessageConsoleException If the number is negative or zero.
   **/
  public final void setIgnoreSampleCount(int n)
    throws DisplayMessageConsoleException {
    if (n <= 0) {
      throw new DisplayMessageConsoleException(
	"ignoreLessThanOneError.text",
	"You must ignore at least the first sample");
    }

    setIgnoreSampleCountInternal(n);
  }

  private final void setIgnoreSampleCountInternal(int n) {
    if (n != m_ignoreSampleCount) {
      final int old = m_ignoreSampleCount;
      m_ignoreSampleCount = n;
      m_changeSupport.firePropertyChange(IGNORE_SAMPLES_PROPERTY,
					 old, m_ignoreSampleCount);
    }
  }

  /**
   * Get the sample interval.
   *
   * @return The interval in milliseconds.
   **/
  public final int getSampleInterval() {
    return m_sampleInterval;
  }

  /**
   * Set the sample interval.
   *
   * @param interval The interval in milliseconds.
   * @throws DisplayMessageConsoleException If the number is negative or zero.
   **/
  public final void setSampleInterval(int interval)
    throws DisplayMessageConsoleException {
    if (interval <= 0) {
      throw new DisplayMessageConsoleException(
	"intervalLessThanOneError.text",
	"Minimum sample interval is 1 ms");
    }

    setSampleIntervalInternal(interval);
  }

  private final void setSampleIntervalInternal(int interval) {
    if (interval != m_sampleInterval) {
      final int old = m_sampleInterval;
      m_sampleInterval = interval;
      m_changeSupport.firePropertyChange(SAMPLE_INTERVAL_PROPERTY,
					 old, m_sampleInterval);
    }
  }

  /**
   * Get the number of significant figures.
   *
   * @return The number of significant figures.
   **/
  public final int getSignificantFigures() {
    return m_significantFigures;
  }

  /**
   * Set the number of significant figures.
   *
   * @param n The number of significant figures.
   * @throws DisplayMessageConsoleException If the number is negative.
   **/
  public final void setSignificantFigures(int n)
    throws DisplayMessageConsoleException {
    if (n <= 0) {
      throw new DisplayMessageConsoleException(
	"significantFiguresNegativeError.text",
	"Number of significant figures cannot be negative");
    }

    setSignificantFiguresInternal(n);
  }

  private final void setSignificantFiguresInternal(int n) {
    if (n != m_significantFigures) {
      final int old = m_significantFigures;
      m_significantFigures = n;
      m_changeSupport.firePropertyChange(SIG_FIG_PROPERTY,
					 old, m_significantFigures);
    }
  }

  /**
   * Get the console address as a string.
   *
   * @return The address.
   **/
  public final String getConsoleAddress() {
    return m_consoleAddressString;
  }

  /**
   * Set the console address.
   *
   * @param s Either a machine name or the IP address.
   * @throws DisplayMessageConsoleException If the address is not
   * valid.
   **/
  public final void setConsoleAddress(String s)
    throws DisplayMessageConsoleException {
    // We treat any non-multicast address that we can look up as
    // valid. I guess we could also try binding to it to discover
    // whether it is local, but that could take an indeterminate
    // amount of time.

    if (s.length() > 0) {	// Empty string => all local hosts.
      final InetAddress newAddress;

      try {
	newAddress = InetAddress.getByName(s);
      }
      catch (UnknownHostException e) {
	throw new DisplayMessageConsoleException(
	  "unknownHostError.text", "Unknown hostname");
      }

      if (newAddress.isMulticastAddress()) {
	throw new DisplayMessageConsoleException(
	  "invalidConsoleAddressError.text",
	  "Invalid console address");
      }
    }

    setConsoleAddressInternal(s);
  }

  private final void setConsoleAddressInternal(String s) {
    if (!s.equals(m_consoleAddressString)) {
      final String old = m_consoleAddressString;
      m_consoleAddressString = s;
      m_changeSupport.firePropertyChange(CONSOLE_ADDRESS_PROPERTY,
					 old, m_consoleAddressString);
    }
  }

  /**
   * Get the console port.
   *
   * @return The port.
   **/
  public final int getConsolePort() {
    return m_consolePort;
  }

  /**
   * Set the console port.
   *
   * @param i The port number.
   * @throws DisplayMessageConsoleException If the port number is not sensible.
   **/
  public final void setConsolePort(int i)
    throws DisplayMessageConsoleException {
    assertValidPort(i);
    setConsolePortInternal(i);
  }

  private final void setConsolePortInternal(int i) {
    if (i != m_consolePort) {
      final int old = m_consolePort;
      m_consolePort = i;
      m_changeSupport.firePropertyChange(CONSOLE_PORT_PROPERTY,
					 old, m_consolePort);
    }
  }

  /**
   * Get the grinder process multicast address as a string.
   *
   * @return The address.
   **/
  public final String getGrinderAddress() {
    return m_grinderAddressString;
  }

  /**
   * Set the grinder process multicast address.
   *
   * @param s Either a machine name or the IP address.
   * @throws DisplayMessageConsoleException If the multicast address is
   * not valid.
   **/
  public final void setGrinderAddress(String s)
    throws DisplayMessageConsoleException {
    final InetAddress newAddress;

    try {
      newAddress = InetAddress.getByName(s);
    }
    catch (UnknownHostException e) {
      throw new DisplayMessageConsoleException(
	"unknownHostError.text", "Unknown hostname");
    }

    if (!newAddress.isMulticastAddress()) {
      throw new DisplayMessageConsoleException(
	"invalidGrinderAddressError.text",
	"Invalid multicast address");
    }

    setGrinderAddressInternal(s);
  }

  private final void setGrinderAddressInternal(String s) {
    if (!s.equals(m_grinderAddressString)) {
      final String old = m_grinderAddressString;
      m_grinderAddressString = s;
      m_changeSupport.firePropertyChange(GRINDER_ADDRESS_PROPERTY,
					 old, m_grinderAddressString);
    }
  }

  /**
   * Get the grinder process multicast port.
   *
   * @return The port.
   **/
  public final int getGrinderPort() {
    return m_grinderPort;
  }

  /**
   * Set the grinder process multicast port.
   *
   * @param port The port number.
   * @throws DisplayMessageConsoleException If the port number is not sensible.
   **/
  public final void setGrinderPort(int port)
    throws DisplayMessageConsoleException {
    assertValidPort(port);
    setGrinderPortInternal(port);
  }

  private final void setGrinderPortInternal(int port) {
    if (port != m_grinderPort) {
      final int old = m_grinderPort;
      m_grinderPort = port;
      m_changeSupport.firePropertyChange(GRINDER_PORT_PROPERTY,
					 old, m_grinderPort);
    }
  }

  private final void assertValidPort(int port)
    throws DisplayMessageConsoleException {
    if (port < 0 || port > CommunicationDefaults.MAX_PORT) {
      throw new DisplayMessageConsoleException(
	"invalidPortNumberError.text", 
	"Port numbers should be in the range [0, 65535]");
    }
  }

  /**
   * Get whether the console should be reset with the worker
   * processes.
   *
   * @return <code>true</code> => the console should be reset with the worker processes.
   */
  public final boolean getResetConsoleWithProcesses() {
    return m_resetConsoleWithProcesses;
  }

  /**
   * Set whether the console should be reset with the worker
   * processes.
   *
   * @param b <code>true</code> => the console should be reset with the worker processes.
   */
  public final void setResetConsoleWithProcesses(boolean b) {

    if (b != m_resetConsoleWithProcesses) {
      final boolean old = m_resetConsoleWithProcesses;
      m_resetConsoleWithProcesses = b;

      m_changeSupport.firePropertyChange(RESET_CONSOLE_WITH_PROCESSES_PROPERTY,
					 old, m_resetConsoleWithProcesses);
    }
  }

  /**
   * Get whether the user wants to be asked if console should be reset
   * with the worker processes.
   *
   * @return <code>true</code> => the user wants to be asked.
   */
  public final boolean getResetConsoleWithProcessesDontAsk() {
    return m_resetConsoleWithProcessesDontAsk;
  }

  /**
   * Set that the user doesn't want to be asked if console should be
   * reset with the worker processes.
   * @exception GrinderException If the property couldn't be persisted.
   */
  public final void setResetConsoleWithProcessesDontAsk()
    throws GrinderException {

    if (!m_resetConsoleWithProcessesDontAsk) {
      setResetConsoleWithProcessesDontAskInternal(true);

      m_properties.saveSingleProperty(
	RESET_CONSOLE_WITH_PROCESSES_DONT_ASK_PROPERTY, "true");
    }
  }

  private final void setResetConsoleWithProcessesDontAskInternal(boolean b) {

    if (b != m_resetConsoleWithProcessesDontAsk) {
      final boolean old = m_resetConsoleWithProcessesDontAsk;
      m_resetConsoleWithProcessesDontAsk = b;

      m_changeSupport.firePropertyChange(
	RESET_CONSOLE_WITH_PROCESSES_DONT_ASK_PROPERTY,
	old, m_resetConsoleWithProcessesDontAsk);
    }
  }

  public final ScriptDistributionFiles getScriptDistributionFiles() {
    return m_scriptDistributionFiles;
  }

  public final void setScriptDistributionFiles(
    ScriptDistributionFiles scriptDistributionFiles) {

    if (!scriptDistributionFiles.equals(m_scriptDistributionFiles)) {
      final ScriptDistributionFiles old = scriptDistributionFiles;
      m_scriptDistributionFiles = scriptDistributionFiles;
      
      m_changeSupport.firePropertyChange(
	SCRIPT_DISTRIBUTION_FILES_PROPERTY, old, m_scriptDistributionFiles);
    }
  }
}
