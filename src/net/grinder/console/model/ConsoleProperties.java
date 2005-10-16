// Copyright (C) 2001, 2002, 2003, 2004, 2005 Philip Aston
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.util.Directory;


/**
 * Class encapsulating the console options.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ConsoleProperties {

  /** Property name. */
  public static final String COLLECT_SAMPLES_PROPERTY =
    "grinder.console.numberToCollect";

  /** Property name. */
  public static final String IGNORE_SAMPLES_PROPERTY =
    "grinder.console.numberToIgnore";

  /** Property name. */
  public static final String SAMPLE_INTERVAL_PROPERTY =
    "grinder.console.sampleInterval";

  /** Property name. */
  public static final String SIG_FIG_PROPERTY =
    "grinder.console.significantFigures";

  /** Property name. */
  public static final String CONSOLE_HOST_PROPERTY =
    "grinder.console.consoleHost";

  /** Property name. */
  public static final String CONSOLE_PORT_PROPERTY =
    "grinder.console.consolePort";

  /** Property name. */
  public static final String RESET_CONSOLE_WITH_PROCESSES_PROPERTY =
    "grinder.console.resetConsoleWithProcesses";

  /** Property name. */
  public static final String RESET_CONSOLE_WITH_PROCESSES_ASK_PROPERTY =
    "grinder.console.resetConsoleWithProcessesAsk";

  /** Property name. */
  public static final String SCRIPT_NOT_SET_ASK_PROPERTY =
    "grinder.console.scriptNotSetAsk";

  /** Property name. */
  public static final String START_WITH_UNSAVED_BUFFERS_ASK_PROPERTY =
    "grinder.console.startWithUnsavedBuffersAsk";

  /** Property name. */
  public static final String STOP_PROCESSES_ASK_PROPERTY =
    "grinder.console.stopProcessesAsk";

  /** Property name. */
  public static final String SCRIPT_FILE_PROPERTY =
    "grinder.console.scriptFile";

  /** Property name. */
  public static final String DISTRIBUTION_DIRECTORY_PROPERTY =
    "grinder.console.scriptDistributionDirectory";

  /** Property name. */
  public static final String DISTRIBUTION_FILE_FILTER_EXPRESSION_PROPERTY =
    "grinder.console.distributionFileFilterExpression";

  /** Property name. */
  public static final String SCAN_DISTRIBUTION_FILES_PERIOD_PROPERTY =
    "grinder.console.scanDistributionFilesPeriod";

  /** Property name. */
  public static final String LOOK_AND_FEEL_PROPERTY =
    "grinder.console.lookAndFeel";

  private final PropertyChangeSupport m_changeSupport =
    new PropertyChangeSupport(this);

  private int m_collectSampleCount;
  private int m_ignoreSampleCount;
  private int m_sampleInterval;
  private int m_significantFigures;
  private boolean m_resetConsoleWithProcesses;
  private File m_scriptFile;
  private Directory m_distributionDirectory;
  private Pattern m_distributionFileFilterPattern;
  private int m_scanDistributionFilesPeriod;
  private String m_lookAndFeel;

  private AskBoolean m_resetConsoleWithProcessesAsk =
    new AskBoolean(RESET_CONSOLE_WITH_PROCESSES_ASK_PROPERTY);

  private AskBoolean m_scriptNotSetAsk =
    new AskBoolean(SCRIPT_NOT_SET_ASK_PROPERTY);

  private AskBoolean m_startWithUnsavedBuffersAsk =
    new AskBoolean(START_WITH_UNSAVED_BUFFERS_ASK_PROPERTY);

  private AskBoolean m_stopProcessesAsk =
    new AskBoolean(STOP_PROCESSES_ASK_PROPERTY);

  /**
   *We hang onto the host as a string so we can copy and externalise
   *it reasonably.
   */
  private String m_consoleHostString;
  private int m_consolePort;

  private final Resources m_resources;

  /**
   * Use to save and load properties, and to keep track of the
   * associated file.
   */
  private final GrinderProperties m_properties;

  /**
   * Construct a ConsoleProperties backed by the given file.
   *
   * @param resources Console resources.
   * @param file The properties file.
   * @exception DisplayMessageConsoleException If the properties file
   * cannot be read or the properties file contains invalid data.
   *
   */
  public ConsoleProperties(Resources resources, File file)
    throws DisplayMessageConsoleException {

    m_resources = resources;

    try {
      m_properties = new GrinderProperties(file);
    }
    catch (GrinderProperties.PersistenceException e) {
      throw new DisplayMessageConsoleException(
        m_resources, "couldNotLoadOptionsError.text", e);
    }

    setCollectSampleCount(
      m_properties.getInt(COLLECT_SAMPLES_PROPERTY, 0));
    setIgnoreSampleCount(m_properties.getInt(IGNORE_SAMPLES_PROPERTY, 0));
    setSampleInterval(m_properties.getInt(SAMPLE_INTERVAL_PROPERTY, 1000));
    setSignificantFigures(m_properties.getInt(SIG_FIG_PROPERTY, 3));

    setConsoleHost(
      m_properties.getProperty(CONSOLE_HOST_PROPERTY,
                               CommunicationDefaults.CONSOLE_HOST));

    setConsolePort(
      m_properties.getInt(CONSOLE_PORT_PROPERTY,
                          CommunicationDefaults.CONSOLE_PORT));

    setResetConsoleWithProcesses(
      m_properties.getBoolean(RESET_CONSOLE_WITH_PROCESSES_PROPERTY, false));

    setScriptFile(m_properties.getFile(SCRIPT_FILE_PROPERTY, null));

    setDistributionDirectory(
      m_properties.getFile(DISTRIBUTION_DIRECTORY_PROPERTY, null));

    setDistributionFileFilterExpression(
      m_properties.getProperty(
        DISTRIBUTION_FILE_FILTER_EXPRESSION_PROPERTY, null));

    setScanDistributionFilesPeriodInternal(
      m_properties.getInt(SCAN_DISTRIBUTION_FILES_PERIOD_PROPERTY, 6000));

    setLookAndFeel(m_properties.getProperty(LOOK_AND_FEEL_PROPERTY, null));

    m_resetConsoleWithProcessesAsk.setFromProperties();
    m_scriptNotSetAsk.setFromProperties();
    m_startWithUnsavedBuffersAsk.setFromProperties();
    m_stopProcessesAsk.setFromProperties();
  }

  /**
   * Copy constructor. Does not copy property change listeners.
   *
   * @param properties The properties to copy.
   */
  public ConsoleProperties(ConsoleProperties properties) {
    m_resources = properties.m_resources;
    m_properties = properties.m_properties;
    set(properties);
  }

  /**
   * Assignment. Does not copy property change listeners, nor the
   * associated file.
   *
   * @param properties The properties to copy.
   */
  public void set(ConsoleProperties properties) {
    setCollectSampleCountInternal(properties.getCollectSampleCount());
    setIgnoreSampleCountInternal(properties.getIgnoreSampleCount());
    setSampleIntervalInternal(properties.getSampleInterval());
    setSignificantFiguresInternal(properties.getSignificantFigures());
    setConsoleHostInternal(properties.getConsoleHost());
    setConsolePortInternal(properties.getConsolePort());
    setResetConsoleWithProcesses(properties.getResetConsoleWithProcesses());
    setScriptFile(properties.getScriptFile());
    setDistributionDirectory(properties.getDistributionDirectory());
    setDistributionFileFilterPattern(
      properties.getDistributionFileFilterPattern());
    setScanDistributionFilesPeriodInternal(
      properties.getScanDistributionFilesPeriod());
    setLookAndFeel(properties.getLookAndFeel());

    m_resetConsoleWithProcessesAsk.set(
      properties.getResetConsoleWithProcessesAsk());
    m_scriptNotSetAsk.set(properties.getScriptNotSetAsk());
    m_startWithUnsavedBuffersAsk.set(
      properties.getStartWithUnsavedBuffersAsk());
    m_stopProcessesAsk.set(properties.getStopProcessesAsk());
  }

  /**
   * Add a <code>PropertyChangeListener</code>.
   *
   * @param listener The listener.
   */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    m_changeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Add a <code>PropertyChangeListener</code> which listens to a
   * particular property.
   *
   * @param property The property.
   * @param listener The listener.
   */
  public void addPropertyChangeListener(
    String property, PropertyChangeListener listener) {
    m_changeSupport.addPropertyChangeListener(property, listener);
  }

  /**
   * Save to the associated file.
   *
   * @exception DisplayMessageConsoleException If an error occurs.
   */
  public void save() throws DisplayMessageConsoleException {
    m_properties.setInt(COLLECT_SAMPLES_PROPERTY, m_collectSampleCount);
    m_properties.setInt(IGNORE_SAMPLES_PROPERTY, m_ignoreSampleCount);
    m_properties.setInt(SAMPLE_INTERVAL_PROPERTY, m_sampleInterval);
    m_properties.setInt(SIG_FIG_PROPERTY, m_significantFigures);
    m_properties.setProperty(CONSOLE_HOST_PROPERTY, m_consoleHostString);
    m_properties.setInt(CONSOLE_PORT_PROPERTY, m_consolePort);
    m_properties.setBoolean(RESET_CONSOLE_WITH_PROCESSES_PROPERTY,
                            m_resetConsoleWithProcesses);
    if (m_scriptFile != null) {
      m_properties.setFile(SCRIPT_FILE_PROPERTY, m_scriptFile);
    }

    m_properties.setFile(DISTRIBUTION_DIRECTORY_PROPERTY,
      m_distributionDirectory.getFile());

    m_properties.setProperty(DISTRIBUTION_FILE_FILTER_EXPRESSION_PROPERTY,
      m_distributionFileFilterPattern.pattern());

    m_properties.setInt(SCAN_DISTRIBUTION_FILES_PERIOD_PROPERTY,
      m_scanDistributionFilesPeriod);

    if (m_lookAndFeel != null) {
      m_properties.setProperty(LOOK_AND_FEEL_PROPERTY, m_lookAndFeel);
    }

    m_resetConsoleWithProcessesAsk.setToProperties();
    m_scriptNotSetAsk.setToProperties();
    m_startWithUnsavedBuffersAsk.setToProperties();
    m_stopProcessesAsk.setToProperties();

    try {
      m_properties.save();
    }
    catch (GrinderProperties.PersistenceException e) {
      throw new DisplayMessageConsoleException(
          m_resources, "couldNotSaveOptionsError.text", e);
    }
  }

  /**
   * Get the number of samples to collect.
   *
   * @return The number.
   */
  public int getCollectSampleCount() {
    return m_collectSampleCount;
  }

  /**
   * Set the number of samples to collect.
   *
   * @param n The number. 0 => forever.
   * @throws DisplayMessageConsoleException If the number is negative.
   */
  public void setCollectSampleCount(int n)
    throws DisplayMessageConsoleException {

    if (n < 0) {
      throw new DisplayMessageConsoleException(
        m_resources, "collectNegativeError.text");
    }

    setCollectSampleCountInternal(n);
  }

  private void setCollectSampleCountInternal(int n) {
    final int old = m_collectSampleCount;
    m_collectSampleCount = n;
    m_changeSupport.firePropertyChange(COLLECT_SAMPLES_PROPERTY,
                                       old, m_collectSampleCount);
  }

  /**
   * Get the number of samples to ignore.
   *
   * @return The number.
   */
  public int getIgnoreSampleCount() {
    return m_ignoreSampleCount;
  }

  /**
   * Set the number of samples to collect.
   *
   * @param n The number. Must be positive.
   * @throws DisplayMessageConsoleException If the number is negative or zero.
   */
  public void setIgnoreSampleCount(int n)
    throws DisplayMessageConsoleException {

    if (n < 0) {
      throw new DisplayMessageConsoleException(
        m_resources, "ignoreSamplesNegativeError.text");
    }

    setIgnoreSampleCountInternal(n);
  }

  private void setIgnoreSampleCountInternal(int n) {
    final int old = m_ignoreSampleCount;
    m_ignoreSampleCount = n;
    m_changeSupport.firePropertyChange(IGNORE_SAMPLES_PROPERTY,
                                       old, m_ignoreSampleCount);
  }

  /**
   * Get the sample interval.
   *
   * @return The interval in milliseconds.
   */
  public int getSampleInterval() {
    return m_sampleInterval;
  }

  /**
   * Set the sample interval.
   *
   * @param interval The interval in milliseconds.
   * @throws DisplayMessageConsoleException If the number is negative or zero.
   */
  public void setSampleInterval(int interval)
    throws DisplayMessageConsoleException {

    if (interval <= 0) {
      throw new DisplayMessageConsoleException(
        m_resources, "intervalLessThanOneError.text");
    }

    setSampleIntervalInternal(interval);
  }

  private void setSampleIntervalInternal(int interval) {
    final int old = m_sampleInterval;
    m_sampleInterval = interval;
    m_changeSupport.firePropertyChange(SAMPLE_INTERVAL_PROPERTY,
                                       old, m_sampleInterval);
  }

  /**
   * Get the number of significant figures.
   *
   * @return The number of significant figures.
   */
  public int getSignificantFigures() {
    return m_significantFigures;
  }

  /**
   * Set the number of significant figures.
   *
   * @param n The number of significant figures.
   * @throws DisplayMessageConsoleException If the number is negative.
   */
  public void setSignificantFigures(int n)
    throws DisplayMessageConsoleException {

    if (n <= 0) {
      throw new DisplayMessageConsoleException(
        m_resources, "significantFiguresNegativeError.text");
    }

    setSignificantFiguresInternal(n);
  }

  private void setSignificantFiguresInternal(int n) {
    final int old = m_significantFigures;
    m_significantFigures = n;
    m_changeSupport.firePropertyChange(SIG_FIG_PROPERTY,
                                       old, m_significantFigures);
  }

  /**
   * Get the console host as a string.
   *
   * @return The address.
   */
  public String getConsoleHost() {
    return m_consoleHostString;
  }

  /**
   * Set the console host.
   *
   * @param s Either a machine name or the IP address.
   * @throws DisplayMessageConsoleException If the address is not
   * valid.
   */
  public void setConsoleHost(String s) throws DisplayMessageConsoleException {
    // We treat any address that we can look up as valid. I guess we
    // could also try binding to it to discover whether it is local,
    // but that could take an indeterminate amount of time.

    if (s.length() > 0) {    // Empty string => all local hosts.
      final InetAddress newAddress;

      try {
        newAddress = InetAddress.getByName(s);
      }
      catch (UnknownHostException e) {
        throw new DisplayMessageConsoleException(
          m_resources, "unknownHostError.text");
      }

      if (newAddress.isMulticastAddress()) {
        throw new DisplayMessageConsoleException(
          m_resources, "invalidConsoleHostError.text");
      }
    }

    setConsoleHostInternal(s);
  }

  private void setConsoleHostInternal(String s) {
    final String old = m_consoleHostString;
    m_consoleHostString = s;
    m_changeSupport.firePropertyChange(CONSOLE_HOST_PROPERTY,
                                       old, m_consoleHostString);
  }

  /**
   * Get the console port.
   *
   * @return The port.
   */
  public int getConsolePort() {
    return m_consolePort;
  }

  /**
   * Set the console port.
   *
   * @param i The port number.
   * @throws DisplayMessageConsoleException If the port number is not sensible.
   */
  public void setConsolePort(int i) throws DisplayMessageConsoleException {
    assertValidPort(i);
    setConsolePortInternal(i);
  }

  private void setConsolePortInternal(int i) {
    final int old = m_consolePort;
    m_consolePort = i;
    m_changeSupport.firePropertyChange(CONSOLE_PORT_PROPERTY,
                                       old, m_consolePort);
  }

  private void assertValidPort(int port)
    throws DisplayMessageConsoleException {

    if (port < CommunicationDefaults.MIN_PORT ||
        port > CommunicationDefaults.MAX_PORT) {
      throw new DisplayMessageConsoleException(
        m_resources,
        "invalidPortNumberError.text",
        new Object[] {
          new Integer(CommunicationDefaults.MIN_PORT),
          new Integer(CommunicationDefaults.MAX_PORT), }
        );
    }
  }

  /**
   * Get whether the console should be reset with the worker
   * processes.
   *
   * @return <code>true</code> => the console should be reset with the
   * worker processes.
   */
  public boolean getResetConsoleWithProcesses() {
    return m_resetConsoleWithProcesses;
  }

  /**
   * Set whether the console should be reset with the worker
   * processes.
   *
   * @param b <code>true</code> => the console should be reset with
   * the worker processes.
   */
  public void setResetConsoleWithProcesses(boolean b) {

    final boolean old = m_resetConsoleWithProcesses;
    m_resetConsoleWithProcesses = b;

    m_changeSupport.firePropertyChange(RESET_CONSOLE_WITH_PROCESSES_PROPERTY,
                                       old, m_resetConsoleWithProcesses);
  }

  /**
   * Get whether the user wants to be asked if console should be reset
   * with the worker processes.
   *
   * @return <code>true</code> => the user wants to be asked.
   */
  public boolean getResetConsoleWithProcessesAsk() {
    return m_resetConsoleWithProcessesAsk.get();
  }

  /**
   * Set whether the user wants to be asked if console should be reset
   * with the worker processes.
   *
   * @param value <code>true</code> => the user wants to be asked.
   * @exception DisplayMessageConsoleException If the property
   * couldn't be persisted
   */
  public void setResetConsoleWithProcessesAsk(boolean value)
    throws DisplayMessageConsoleException {
    m_resetConsoleWithProcessesAsk.set(value);
    m_resetConsoleWithProcessesAsk.save();
  }

  /**
   * Get whether the user wants to be asked if console should be reset
   * with the worker processes.
   *
   * @return <code>true</code> => the user wants to be asked.
   */
  public boolean getScriptNotSetAsk() {
    return m_scriptNotSetAsk.get();
  }

  /**
   * Set whether the user wants to be asked if console should be reset
   * with the worker processes.
   *
   * @param value <code>true</code> => the user wants to be asked.
   * @exception DisplayMessageConsoleException If the property
   * couldn't be persisted.
   */
  public void setScriptNotSetAsk(boolean value)
    throws DisplayMessageConsoleException {
    m_scriptNotSetAsk.set(value);
    m_scriptNotSetAsk.save();
  }


  /**
   * Get whether the user wants to be warned when starting processes
   * with unsaved buffers.
   *
   * @return <code>true</code> => the user wants to be warned.
   */
  public boolean getStartWithUnsavedBuffersAsk() {
    return m_startWithUnsavedBuffersAsk.get();
  }

  /**
   * Set whether the user wants to be warned when starting processes
   * with unsaved buffers.
   *
   * @param value <code>true</code> => the user wants to be warned.
   * @exception DisplayMessageConsoleException If the property
   * couldn't be persisted.
   */
  public void setStartWithUnsavedBuffersAsk(boolean value)
    throws DisplayMessageConsoleException {
    m_startWithUnsavedBuffersAsk.set(value);
    m_startWithUnsavedBuffersAsk.save();
  }

  /**
   * Get whether the user wants to be asked to confirm that processes
   * should be stopped.
   *
   * @return <code>true</code> => the user wants to be asked.
   */
  public boolean getStopProcessesAsk() {
    return m_stopProcessesAsk.get();
  }

  /**
   * Set whether the user wants to be asked to confirm that processes
   * should be stopped.
   *
   * @param value <code>true</code> => the user wants to be asked.
   * @exception DisplayMessageConsoleException If the property
   * couldn't be persisted.
   */
  public void setStopProcessesAsk(boolean value)
    throws DisplayMessageConsoleException {
    m_stopProcessesAsk.set(value);
    m_stopProcessesAsk.save();
  }

  /**
   * Get the script file.
   *
   * @return The script file. <code>null</code> => No file set.
   */
  public File getScriptFile() {
    return m_scriptFile;
  }

  /**
   * Set the script file.
   *
   * @param scriptFile The script file. <code>null</code> => No file
   * set.
   */
  public void setScriptFile(File scriptFile) {

    final File old = m_scriptFile;
    m_scriptFile = scriptFile;
    m_changeSupport.firePropertyChange(
      SCRIPT_FILE_PROPERTY, old, m_scriptFile);
  }

  /**
   * Get the script distribution directory.
   *
   * @return The directory.
   */
  public Directory getDistributionDirectory() {
    return m_distributionDirectory;
  }

  /**
   * Set the script distribution directory from a File.
   *
   * @param distributionDirectory The directory. <code>null</code> =>
   * default to local directory.
   */
  private void setDistributionDirectory(File distributionDirectory) {

    if (distributionDirectory == null) {
      setDistributionDirectory(new Directory());
    }
    else {
      try {
        setDistributionDirectory(new Directory(distributionDirectory));
      }
      catch (Directory.DirectoryException e) {
        setDistributionDirectory(new Directory());
      }
    }
  }

  /**
   * Set the script distribution directory.
   *
   * @param distributionDirectory The directory.
   */
  public void setDistributionDirectory(Directory distributionDirectory) {
    final Directory old = m_distributionDirectory;
    m_distributionDirectory = distributionDirectory;

    m_changeSupport.firePropertyChange(
      DISTRIBUTION_DIRECTORY_PROPERTY, old, m_distributionDirectory);
  }

  /**
   * Save the distribution directory property value to the user's
   * preferences file.
   *
   * @throws DisplayMessageConsoleException If the property could not be saved.
   */
  public void saveDistributionDirectory()
    throws DisplayMessageConsoleException {
    m_properties.setFile(DISTRIBUTION_DIRECTORY_PROPERTY,
                         m_distributionDirectory.getFile());

    try {
      m_properties.saveSingleProperty(DISTRIBUTION_DIRECTORY_PROPERTY);
    }
    catch (GrinderProperties.PersistenceException e) {
      throw new DisplayMessageConsoleException(
        m_resources, "couldNotSaveOptionsError.text", e);
    }
  }

  /**
   * Get the distribution file filter pattern.
   *
   * <p>The original regular expression can be obtained with
   * <code>getDistributionFileFilterPattern().getPattern</code>.</p>
   *
   * @return The pattern.
   * @see #setDistributionFileFilterExpression
   */
  public Pattern getDistributionFileFilterPattern() {
    return m_distributionFileFilterPattern;
  }

  /**
   * Set the distribution file filter regular expression.
   *
   * <p>Files and directory names (not full paths) that match the
   * regular expression are not distributed. Directory names are
   * distinguished by a trailing '/'. The expression is in Perl 5
   * format.</p>
   *
   * @param expression A Perl 5 format expression. <code>null</code>
   * => use default pattern.
   * @throws DisplayMessageConsoleException If the pattern is not
   * valid.
   */
  public void setDistributionFileFilterExpression(String expression)
    throws DisplayMessageConsoleException {

    try {
      if (expression == null) {
        setDistributionFileFilterPattern(
          Pattern.compile(
            ConsolePropertyDefaults.DISTRIBUTION_FILE_FILTER_EXPRESSION));
      }
      else {
        setDistributionFileFilterPattern(Pattern.compile(expression));
      }
    }
    catch (PatternSyntaxException e) {
      throw new DisplayMessageConsoleException(
        m_resources, "distributionFileFilterExpressionError.text", e);
    }
  }

  private void setDistributionFileFilterPattern(Pattern pattern) {
    final Pattern old = m_distributionFileFilterPattern;
    m_distributionFileFilterPattern = pattern;

    m_changeSupport.firePropertyChange(
      DISTRIBUTION_FILE_FILTER_EXPRESSION_PROPERTY,
      old,
      m_distributionFileFilterPattern);
  }

  /**
   * Get the period at which the distribution files should be scanned.
   *
   * @return The period, in milliseconds.
   */
  public int getScanDistributionFilesPeriod() {
    return m_scanDistributionFilesPeriod;
  }

  /**
   * Set the console port.
   *
   * @param i The port number.
   * @throws DisplayMessageConsoleException If the port number is not sensible.
   */
  public void setScanDistributionFilesPeriod(int i)
    throws DisplayMessageConsoleException {

    if (i < 0) {
      throw new DisplayMessageConsoleException(
        m_resources, "scanDistributionFilesPeriodNegativeError.text");
    }

    setScanDistributionFilesPeriodInternal(i);
  }

  private void setScanDistributionFilesPeriodInternal(int i) {
    final int old = m_scanDistributionFilesPeriod;
    m_scanDistributionFilesPeriod = i;
    m_changeSupport.firePropertyChange(SCAN_DISTRIBUTION_FILES_PERIOD_PROPERTY,
                                       old, m_scanDistributionFilesPeriod);
  }

  /**
   * Get the name of the Look and Feel. It is up to the UI
   * implementation how this is interpreted.
   *
   * @return The Look and Feel name. <code>null</code> => use default.
   */
  public String getLookAndFeel() {
    return m_lookAndFeel;
  }

  /**
   * Set the name of the Look and Feel.
   *
   * @param lookAndFeel The Look and Feel name. <code>null</code> =>
   * use default.
   */
  public void setLookAndFeel(String lookAndFeel) {

    final String old = m_lookAndFeel;
    m_lookAndFeel = lookAndFeel;
    m_changeSupport.firePropertyChange(
      LOOK_AND_FEEL_PROPERTY, old, m_lookAndFeel);
  }

  private final class AskBoolean {
    private final String m_propertyName;
    private boolean m_value = true;

    public AskBoolean(String propertyName) {
      m_propertyName = propertyName;
    }

    public void setFromProperties() {
      set(m_properties.getBoolean(m_propertyName, true));
    }

    public void setToProperties() {
      m_properties.setBoolean(m_propertyName, m_value);
    }

    public boolean get() {
      return m_value;
    }

    public void set(boolean b) {
      final boolean old = m_value;
      m_value = b;

      m_changeSupport.firePropertyChange(m_propertyName, old, m_value);
    }

    public void save() throws DisplayMessageConsoleException {
      setToProperties();

      try {
        m_properties.saveSingleProperty(m_propertyName);
      }
      catch (GrinderProperties.PersistenceException e) {
        throw new DisplayMessageConsoleException(
          m_resources, "couldNotSaveOptionsError.text", e);
      }
    }
  }
}
