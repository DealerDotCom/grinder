// Copyright (C) 2001 - 2012 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.model;

import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.util.Directory;


/**
 * Class encapsulating the console options.
 *
 * <p>Adds fixed interface and listener mechanism, but delegates to
 * {@link GrinderProperties} for storage.</p>
 *
 * <p>Implements the read-only part of {@code Map<String, Object>}.</p>
 *
 * @author Philip Aston
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
  public static final String HTTP_HOST_PROPERTY =
    "grinder.console.httpHost";

  /** Property name. */
  public static final String HTTP_PORT_PROPERTY =
    "grinder.console.httpPort";

  /** Property name. */
  public static final String RESET_CONSOLE_WITH_PROCESSES_PROPERTY =
    "grinder.console.resetConsoleWithProcesses";

  /** Property name. */
  public static final String RESET_CONSOLE_WITH_PROCESSES_ASK_PROPERTY =
    "grinder.console.resetConsoleWithProcessesAsk";

  /** Property name. */
  public static final String PROPERTIES_NOT_SET_ASK_PROPERTY =
    "grinder.console.propertiesNotSetAsk";

  /** Property name. */
  public static final String START_WITH_UNSAVED_BUFFERS_ASK_PROPERTY =
    "grinder.console.startWithUnsavedBuffersAsk";

  /** Property name. */
  public static final String STOP_PROCESSES_ASK_PROPERTY =
    "grinder.console.stopProcessesAsk";

  /** Property name. */
  public static final String DISTRIBUTE_ON_START_ASK_PROPERTY =
    "grinder.console.distributeAutomaticallyAsk";

  /** Property name. */
  public static final String PROPERTIES_FILE_PROPERTY =
    "grinder.console.propertiesFile";

  /** Property name. */
  public static final String DISTRIBUTION_DIRECTORY_PROPERTY =
    "grinder.console.scriptDistributionDirectory";

  /** Property name. */
  public static final String DISTRIBUTION_FILE_FILTER_EXPRESSION_PROPERTY =
    "grinder.console.distributionFileFilterExpression";

  /**
   * Default regular expression for filtering distribution files.
   */
  public static final String DEFAULT_DISTRIBUTION_FILE_FILTER_EXPRESSION =
    "^CVS/$|" +
    "^\\.svn/$|" +
    "^.*~$|" +
    "^(out_|error_|data_)\\w+-\\d+\\.log\\d*$";

  /** Property name. */
  public static final String SCAN_DISTRIBUTION_FILES_PERIOD_PROPERTY =
    "grinder.console.scanDistributionFilesPeriod";

  /** Property name. */
  public static final String LOOK_AND_FEEL_PROPERTY =
    "grinder.console.lookAndFeel";

  /** Property name. */
  public static final String EXTERNAL_EDITOR_COMMAND_PROPERTY =
    "grinder.console.externalEditorCommand";

  /** Property name. */
  public static final String EXTERNAL_EDITOR_ARGUMENTS_PROPERTY =
    "grinder.console.externalEditorArguments";

  /** Property name. */
  public static final String FRAME_BOUNDS_PROPERTY =
    "grinder.console.frameBounds";

  /** Property name. */
  public static final String SAVE_TOTALS_WITH_RESULTS_PROPERTY =
    "grinder.console.saveTotalsWithResults";

  private final PropertyChangeSupport m_changeSupport =
    new PropertyChangeSupport(this);

  private final IntProperty m_collectSampleCount =
    new IntProperty(COLLECT_SAMPLES_PROPERTY, 0);

  private final IntProperty m_ignoreSampleCount =
    new IntProperty(IGNORE_SAMPLES_PROPERTY, 0);

  private final IntProperty m_sampleInterval =
    new IntProperty(SAMPLE_INTERVAL_PROPERTY, 1000);

  private final IntProperty m_significantFigures =
    new IntProperty(SIG_FIG_PROPERTY, 3);

  private final BooleanProperty m_resetConsoleWithProcesses =
    new BooleanProperty(RESET_CONSOLE_WITH_PROCESSES_PROPERTY, false);

  private final FileProperty m_propertiesFile =
    new FileProperty(PROPERTIES_FILE_PROPERTY);

  private final DirectoryProperty m_distributionDirectory =
    new DirectoryProperty(DISTRIBUTION_DIRECTORY_PROPERTY);

  private final PatternProperty m_distributionFileFilterPattern =
    new PatternProperty(
      DISTRIBUTION_FILE_FILTER_EXPRESSION_PROPERTY,
      DEFAULT_DISTRIBUTION_FILE_FILTER_EXPRESSION);

  private final IntProperty m_scanDistributionFilesPeriod =
    new IntProperty(SCAN_DISTRIBUTION_FILES_PERIOD_PROPERTY, 6000);

  private final StringProperty m_lookAndFeel =
    new StringProperty(LOOK_AND_FEEL_PROPERTY, null);

  private final FileProperty m_externalEditorCommand =
    new FileProperty(EXTERNAL_EDITOR_COMMAND_PROPERTY);

  private final StringProperty m_externalEditorArguments =
    new StringProperty(EXTERNAL_EDITOR_ARGUMENTS_PROPERTY, null);

  private final RectangleProperty m_frameBounds =
    new RectangleProperty(FRAME_BOUNDS_PROPERTY);

  private final BooleanProperty m_resetConsoleWithProcessesAsk =
    new BooleanProperty(RESET_CONSOLE_WITH_PROCESSES_ASK_PROPERTY, true);

  private final BooleanProperty m_propertiesNotSetAsk =
    new BooleanProperty(PROPERTIES_NOT_SET_ASK_PROPERTY, true);

  private final BooleanProperty m_startWithUnsavedBuffersAsk =
    new BooleanProperty(START_WITH_UNSAVED_BUFFERS_ASK_PROPERTY, true);

  private final BooleanProperty m_stopProcessesAsk =
    new BooleanProperty(STOP_PROCESSES_ASK_PROPERTY, true);

  private final BooleanProperty m_distributeOnStartAsk =
    new BooleanProperty(DISTRIBUTE_ON_START_ASK_PROPERTY, true);

  private final StringProperty m_consoleHost =
    new StringProperty(CONSOLE_HOST_PROPERTY,
                       CommunicationDefaults.CONSOLE_HOST);

  private final IntProperty m_consolePort =
    new IntProperty(CONSOLE_PORT_PROPERTY, CommunicationDefaults.CONSOLE_PORT);

  private final StringProperty m_httpHost =
      new StringProperty(HTTP_HOST_PROPERTY,
                         CommunicationDefaults.CONSOLE_HOST);

  private final IntProperty m_httpPort =
      new IntProperty(HTTP_PORT_PROPERTY, 6373);

  private final BooleanProperty m_saveTotalsWithResults =
    new BooleanProperty(SAVE_TOTALS_WITH_RESULTS_PROPERTY, false);

  private final Resources m_resources;

  /**
   * We delegate to GrinderProperties for storage and the backing file.
   */
  private final GrinderProperties m_backingProperties;

  /**
   * Construct a ConsoleProperties backed by the given file.
   *
   * @param resources Console resources.
   * @param file The properties file.
   * @throws ConsoleException If the properties file
   * cannot be read or the properties file contains invalid data.
   *
   */
  public ConsoleProperties(Resources resources, File file)
    throws ConsoleException {

    m_resources = resources;

    try {
      m_backingProperties = new GrinderProperties(file);
    }
    catch (GrinderProperties.PersistenceException e) {
      throw new DisplayMessageConsoleException(
        m_resources, "couldNotLoadOptionsError.text", e);
    }
  }

  /**
   * Copy constructor. Does not copy property change listeners.
   *
   * @param properties The properties to copy.
   */
  public ConsoleProperties(ConsoleProperties properties) {
    m_resources = properties.m_resources;
    m_backingProperties = properties.m_backingProperties;
    set(properties);
  }

  /**
   * Assignment. Does not copy property change listeners, nor the
   * associated file.
   *
   * @param properties The properties to copy.
   */
  public void set(ConsoleProperties properties) {
    m_backingProperties.clear();
    m_backingProperties.putAll(properties.m_backingProperties);
  }

  /**
   * Add a {@code PropertyChangeListener}.
   *
   * @param listener The listener.
   */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    m_changeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Add a {@code PropertyChangeListener} which listens to a particular
   * property.
   *
   * @param property
   *          The property.
   * @param listener
   *          The listener.
   */
  public void addPropertyChangeListener(
    String property, PropertyChangeListener listener) {
    m_changeSupport.addPropertyChangeListener(property, listener);
  }

  /**
   * Save to the associated file.
   *
   * @throws ConsoleException If an error occurs.
   */
  public void save() throws ConsoleException {
    try {
      m_backingProperties.save();
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
    return m_collectSampleCount.get();
  }

  /**
   * Set the number of samples to collect.
   *
   * @param n The number. 0 => forever.
   * @throws ConsoleException If the number is negative.
   */
  public void setCollectSampleCount(int n) throws ConsoleException {
    if (n < 0) {
      throw new DisplayMessageConsoleException(
        m_resources, "collectNegativeError.text");
    }

    m_collectSampleCount.set(n);
  }

  /**
   * Get the number of samples to ignore.
   *
   * @return The number.
   */
  public int getIgnoreSampleCount() {
    return m_ignoreSampleCount.get();
  }

  /**
   * Set the number of samples to collect.
   *
   * @param n The number. Must be positive.
   * @throws ConsoleException If the number is negative or zero.
   */
  public void setIgnoreSampleCount(int n) throws ConsoleException {
    if (n < 0) {
      throw new DisplayMessageConsoleException(
        m_resources, "ignoreSamplesNegativeError.text");
    }

    m_ignoreSampleCount.set(n);
  }

  /**
   * Get the sample interval.
   *
   * @return The interval in milliseconds.
   */
  public int getSampleInterval() {
    return m_sampleInterval.get();
  }

  /**
   * Set the sample interval.
   *
   * @param interval The interval in milliseconds.
   * @throws ConsoleException If the number is negative or zero.
   */
  public void setSampleInterval(int interval) throws ConsoleException {
    if (interval <= 0) {
      throw new DisplayMessageConsoleException(
        m_resources, "intervalLessThanOneError.text");
    }

    m_sampleInterval.set(interval);
  }

  /**
   * Get the number of significant figures.
   *
   * @return The number of significant figures.
   */
  public int getSignificantFigures() {
    return m_significantFigures.get();
  }

  /**
   * Set the number of significant figures.
   *
   * @param n The number of significant figures.
   * @throws ConsoleException If the number is negative.
   */
  public void setSignificantFigures(int n) throws ConsoleException {
    if (n <= 0) {
      throw new DisplayMessageConsoleException(
        m_resources, "significantFiguresNegativeError.text");
    }

    m_significantFigures.set(n);
  }

  /**
   * Get the console host as a string.
   *
   * @return The address.
   */
  public String getConsoleHost() {
    return m_consoleHost.get();
  }

  /**
   * Validate a unicast IP address.
   *
   * <p>
   * We treat any address that we can look up as valid. I guess we could also
   * try binding to it to discover whether it is local, but that could take an
   * indeterminate amount of time.
   * </p>
   *
   * @param address The address, as a string.
   * @throws ConsoleException If the address is invalid.
   */
  private void checkAddress(String address) throws ConsoleException {
    if (address.length() > 0) {    // Empty string => all local hosts.
      final InetAddress newAddress;

      try {
        newAddress = InetAddress.getByName(address);
      }
      catch (UnknownHostException e) {
        throw new DisplayMessageConsoleException(
          m_resources, "unknownHostError.text");
      }

      if (newAddress.isMulticastAddress()) {
        throw new DisplayMessageConsoleException(
          m_resources, "invalidHostAddressError.text");
      }
    }
  }

  /**
   * Validate a TCP port.
   *
   ** @param port The port.
   * @throws ConsoleException If the port is invalid.
   */
  private void checkPort(int port) throws ConsoleException {

    if (port < CommunicationDefaults.MIN_PORT ||
        port > CommunicationDefaults.MAX_PORT) {
      throw new DisplayMessageConsoleException(
        m_resources,
        "invalidPortNumberError.text",
        new Object[] {
          CommunicationDefaults.MIN_PORT,
          CommunicationDefaults.MAX_PORT, }
        );
    }
  }

  /**
   * Set the console host.
   *
   * @param s Either a machine name or the IP address.
   * @throws ConsoleException If the address is not
   * valid.
   */
  public void setConsoleHost(String s) throws ConsoleException {
    checkAddress(s);
    m_consoleHost.set(s);
  }

  /**
   * Get the console port.
   *
   * @return The port.
   */
  public int getConsolePort() {
    return m_consolePort.get();
  }

  /**
   * Set the console port.
   *
   * @param i The port number.
   * @throws ConsoleException If the port number is not sensible.
   */
  public void setConsolePort(int i) throws ConsoleException {
    checkPort(i);
    m_consolePort.set(i);
  }

  /**
   * Get the HTTP host as a string.
   *
   * @return The address.
   */
  public String getHttpHost() {
    return m_httpHost.get();
  }

  /**
   * Set the HTTP host.
   *
   * @param s Either a machine name or the IP address.
   * @throws ConsoleException If the address is not
   * valid.
   */
  public void setHttpHost(String s) throws ConsoleException {
    checkAddress(s);
    m_httpHost.set(s);
  }

  /**
   * Get the HTTP port.
   *
   * @return The port.
   */
  public int getHttpPort() {
    return m_httpPort.get();
  }

  /**
   * Set the HTTP port.
   *
   * @param i The port number.
   * @throws ConsoleException If the port number is not sensible.
   */
  public void setHttpPort(int i) throws ConsoleException {
    checkPort(i);
    m_httpPort.set(i);
  }

  /**
   * Get whether the console should be reset with the worker
   * processes.
   *
   * @return {@code true} => the console should be reset with the
   * worker processes.
   */
  public boolean getResetConsoleWithProcesses() {
    return m_resetConsoleWithProcesses.get();
  }

  /**
   * Set whether the console should be reset with the worker
   * processes.
   *
   * @param b {@code true} => the console should be reset with
   * the worker processes.
   */
  public void setResetConsoleWithProcesses(boolean b) {
    m_resetConsoleWithProcesses.set(b);
  }

  /**
   * Get whether the user wants to be asked if console should be reset
   * with the worker processes.
   *
   * @return {@code true} => the user wants to be asked.
   */
  public boolean getResetConsoleWithProcessesAsk() {
    return m_resetConsoleWithProcessesAsk.get();
  }

  /**
   * Set and save whether the user wants to be asked if console should be reset
   * with the worker processes.
   *
   * @param value
   *          {@code true} => the user wants to be asked.
   * @throws ConsoleException
   *            If the property couldn't be persisted
   */
  public void setResetConsoleWithProcessesAsk(boolean value)
    throws ConsoleException {
    m_resetConsoleWithProcessesAsk.set(value);
    m_resetConsoleWithProcessesAsk.save();
  }

  /**
   * Get whether the user wants to be asked if console should be reset
   * with the worker processes.
   *
   * @return {@code true} => the user wants to be asked.
   */
  public boolean getPropertiesNotSetAsk() {
    return m_propertiesNotSetAsk.get();
  }

  /**
   * Set and save whether the user wants to be asked if console should be reset
   * with the worker processes.
   *
   * @param value
   *          {@code true} => the user wants to be asked.
   * @throws ConsoleException
   *           If the property couldn't be persisted.
   */
  public void setPropertiesNotSetAsk(boolean value) throws ConsoleException {
    m_propertiesNotSetAsk.set(value);
    m_propertiesNotSetAsk.save();
  }


  /**
   * Get whether the user wants to be warned when starting processes with
   * unsaved buffers.
   *
   * @return {@code true} => the user wants to be warned.
   */
  public boolean getStartWithUnsavedBuffersAsk() {
    return m_startWithUnsavedBuffersAsk.get();
  }

  /**
   * Set and save whether the user wants to be warned when starting processes
   * with unsaved buffers.
   *
   * @param value
   *          {@code true} => the user wants to be warned.
   * @throws ConsoleException
   *           If the property couldn't be persisted.
   */
  public void setStartWithUnsavedBuffersAsk(boolean value)
    throws ConsoleException {
    m_startWithUnsavedBuffersAsk.set(value);
    m_startWithUnsavedBuffersAsk.save();
  }

  /**
   * Get whether the user wants to be asked to confirm that processes
   * should be stopped.
   *
   * @return {@code true} => the user wants to be asked.
   */
  public boolean getStopProcessesAsk() {
    return m_stopProcessesAsk.get();
  }

  /**
   * Set and save whether the user wants to be asked to confirm that processes
   * should be stopped.
   *
   * @param value
   *          {@code true} => the user wants to be asked.
   * @throws ConsoleException If the property couldn't be persisted.
   */
  public void setStopProcessesAsk(boolean value)
    throws ConsoleException {
    m_stopProcessesAsk.set(value);
    m_stopProcessesAsk.save();
  }

  /**
   * Get whether the user wants to distribute files automatically when starting
   * processes.
   *
   * @return {@code true} => the user wants automatic distribution.
   */
  public boolean getDistributeOnStartAsk() {
    return m_distributeOnStartAsk.get();
  }

  /**
   * Set and save whether the user wants to distribute files automatically when
   * starting processes.
   *
   * @param value
   *          {@code true} => the user wants automatic distribution.
   * @throws ConsoleException If the property couldn't be persisted.
   */
  public void setDistributeOnStartAsk(boolean value)
    throws ConsoleException {
    m_distributeOnStartAsk.set(value);
    m_distributeOnStartAsk.save();
  }

  /**
   * Get the properties file.
   *
   * @return The properties file. {@code null} => No file set.
   */
  public File getPropertiesFile() {
    return m_propertiesFile.get();
  }


  /**
   * Set and save the properties file.
   *
   * @param propertiesFile
   *          The properties file. {@code null} => No file set.
   */
  public void setPropertiesFile(File propertiesFile) {
    m_propertiesFile.set(propertiesFile);
  }

  /**
   * Set and save the properties file.
   *
   * @param propertiesFile The properties file. {@code null} => No file
   * set.
   * @throws ConsoleException
   * @throws ConsoleException If the property could not be saved.
   */
  public void setAndSavePropertiesFile(File propertiesFile)
    throws ConsoleException {
    setPropertiesFile(propertiesFile);
    m_propertiesFile.save();
  }

  /**
   * Get the script distribution directory.
   *
   * @return The directory.
   */
  public Directory getDistributionDirectory() {
    return m_distributionDirectory.get();
  }

  /**
   * Set and save the script distribution directory.
   *
   * @param distributionDirectory The directory.
   */
  public void setDistributionDirectory(Directory distributionDirectory) {
    m_distributionDirectory.set(distributionDirectory);
  }


  /**
   * Set and save the script distribution directory.
   *
   * @param distributionDirectory The directory.
   * @throws ConsoleException If the property could not be saved.
   */
  public void setAndSaveDistributionDirectory(Directory distributionDirectory)
    throws ConsoleException {
    setDistributionDirectory(distributionDirectory);
    m_distributionDirectory.save();
  }

  /**
   * Get the distribution file filter pattern.
   *
   * <p>The original regular expression can be obtained with
   * {@link #getDistributionFileFilterPattern()}.</p>
   *
   * @return The pattern.
   * @see #setDistributionFileFilterExpression
   */
  public Pattern getDistributionFileFilterPattern() {
    return m_distributionFileFilterPattern.get();
  }

  /**
   * Get the distribution file filter pattern.
   *
   * <p>The original regular expression can be obtained with
   * {@code getDistributionFileFilterPattern().getPattern()}.</p>
   *
   * @return The pattern.
   * @see #setDistributionFileFilterExpression
   */
  public String getDistributionFileFilterExpression() {
    return getDistributionFileFilterPattern().pattern();
  }

  /**
   * Set the distribution file filter regular expression.
   *
   * <p>Files and directory names (not full paths) that match the
   * regular expression are not distributed. Directory names are
   * distinguished by a trailing '/'. The expression is in Perl 5
   * format.</p>
   *
   * @param expression A Perl 5 format expression. {@code null}
   * => use default pattern.
   * @throws ConsoleException If the pattern is invalid.
   */
  public void setDistributionFileFilterExpression(String expression)
    throws ConsoleException {
    m_distributionFileFilterPattern.setExpression(expression);
  }

  /**
   * Get the period at which the distribution files should be scanned.
   *
   * @return The period, in milliseconds.
   */
  public int getScanDistributionFilesPeriod() {
    return m_scanDistributionFilesPeriod.get();
  }

  /**
   * Set the period at which the distribution files should be scanned.
   *
   * @param i The port number.
   * @throws ConsoleException If the period is negative.
   */
  public void setScanDistributionFilesPeriod(int i) throws ConsoleException {
    if (i < 0) {
      throw new DisplayMessageConsoleException(
        m_resources, "scanDistributionFilesPeriodNegativeError.text");
    }

    m_scanDistributionFilesPeriod.set(i);
  }

  /**
   * Get the name of the Look and Feel. It is up to the UI
   * implementation how this is interpreted.
   *
   * @return The Look and Feel name. {@code null} => use default.
   */
  public String getLookAndFeel() {
    return m_lookAndFeel.get();
  }

  /**
   * Set the name of the Look and Feel.
   *
   * @param lookAndFeel The Look and Feel name. {@code null} =>
   * use default.
   */
  public void setLookAndFeel(String lookAndFeel) {
    m_lookAndFeel.set(lookAndFeel);
  }

  /**
   * Get the external editor command.
   *
   * @return The path to the process to be used for external editing.
   * {@code null} => no external editor set.
   */
  public File getExternalEditorCommand() {
    return m_externalEditorCommand.get();
  }

  /**
   * Set the external editor command.
   *
   * @param command The path to the process to be used for external editing.
   * {@code null} => no external editor set.
   */
  public void setExternalEditorCommand(File command) {
    m_externalEditorCommand.set(command);
  }

  /**
   * Get the external editor arguments.
   *
   * @return The arguments to be used with the external editor.
   */
  public String getExternalEditorArguments() {
    return m_externalEditorArguments.get();
  }

  /**
   * Set the external editor arguments.
   *
   * @param arguments The arguments to be used with the external editor.
   */
  public void setExternalEditorArguments(String arguments) {
    m_externalEditorArguments.set(arguments);
  }

  /**
   * Get the location and size of the console frame.
   *
   * @return The console frame bounds.
   */
  public Rectangle getFrameBounds() {
    return m_frameBounds.get();
  }

  /**
   * Set and save the location and size of the console frame.
   *
   * @param bounds The console frame bounds.
   */
  public void setFrameBounds(Rectangle bounds) {
    m_frameBounds.set(bounds);
  }

  /**
   * Set and save the location and size of the console frame.
   *
   * @param bounds The console frame bounds.
   * @throws ConsoleException If the property couldn't be persisted.
   */
  public void setAndSaveFrameBounds(Rectangle bounds) throws ConsoleException {
    setFrameBounds(bounds);
    m_frameBounds.save();
  }

  /**
   * Get whether saved results files should include the Totals line.
   *
   * @return {@code true} => results files should include totals.
   */
  public boolean getSaveTotalsWithResults() {
    return m_saveTotalsWithResults.get();
  }

  /**
   * Set whether saved results files should include the Totals line.
   *
   * @param b {@code true} => results files should include totals.
   * @throws ConsoleException If the property couldn't be persisted.
   */
  public void setSaveTotalsWithResults(boolean b) throws ConsoleException {
    m_saveTotalsWithResults.set(b);
    m_saveTotalsWithResults.save();
  }

  private abstract class Property<T> {
    private final String m_propertyName;
    private final T m_defaultValue;

    Property(String propertyName, T defaultValue) {
      m_propertyName = propertyName;
      m_defaultValue = defaultValue;
    }

    public final void save() throws ConsoleException {
      try {
        m_backingProperties.saveSingleProperty(m_propertyName);
      }
      catch (GrinderProperties.PersistenceException e) {
        throw new DisplayMessageConsoleException(
          m_resources, "couldNotSaveOptionsError.text", e);
      }
    }

    protected final String getPropertyName() {
      return m_propertyName;
    }

    protected final T getDefaultValue() {
      return m_defaultValue;
    }

    protected abstract T get();

    protected abstract void setToStorage(T value);

    public final void set(T value) {
      final T old = get();

      final T defaultValue = getDefaultValue();

      if (defaultValue == null && value == null ||
          defaultValue != null && defaultValue.equals(value)) {
        m_backingProperties.remove(m_propertyName);
      }
      else {
        setToStorage(value);
      }

      // For some reason, firePropertyChange only suppresses same value
      // updates when the value is not null. The default L&F is null,
      // so this prevents UI flicker on each property change.
      if (old == null && value == null) {
        return;
      }

      m_changeSupport.firePropertyChange(getPropertyName(), old, value);
    }
  }

  private final class StringProperty extends Property<String> {
    public StringProperty(String propertyName, String defaultValue) {
      super(propertyName, defaultValue);
    }

    @Override
    protected String get() {
      return m_backingProperties.getProperty(getPropertyName(),
                                             getDefaultValue());
    }

    @Override
    protected void setToStorage(String value) {
      m_backingProperties.setProperty(getPropertyName(), value);
    }
  }

  private final class PatternProperty extends Property<Pattern> {
    public PatternProperty(String propertyName, String defaultExpression) {
      super(propertyName, Pattern.compile(defaultExpression));
    }

    @Override
    protected Pattern get() {
      final String expression =
        m_backingProperties.getProperty(getPropertyName());

      if (expression != null) {
        try {
          return Pattern.compile(expression);
        }
        catch (PatternSyntaxException e) {
          // Fall through.
        }
      }

      return getDefaultValue();
    }

    @Override
    protected void setToStorage(Pattern value) {
      m_backingProperties.put(getPropertyName(), value.pattern());
    }

    public void setExpression(String expression) throws ConsoleException {
      if (expression == null) {
        m_backingProperties.remove(getPropertyName());
      }
      else {
        try {
          set(Pattern.compile(expression));
        }
        catch (PatternSyntaxException e) {
          throw new DisplayMessageConsoleException(
              m_resources,
              "regularExpressionError.text",
              new Object[] { getPropertyName(), },
              e);
        }
      }
    }
  }

  private final class IntProperty extends Property<Integer> {
    public IntProperty(String propertyName, int defaultValue) {
      super(propertyName, defaultValue);
    }


    @Override
    protected Integer get() {
      return m_backingProperties.getInt(getPropertyName(), getDefaultValue());
    }

    @Override
    protected void setToStorage(Integer value) {
      m_backingProperties.setInt(getPropertyName(), value);
    }
  }

  private final class FileProperty extends Property<File> {
    public FileProperty(String propertyName) {
      super(propertyName, null);
    }

    @Override
    protected File get() {
      return m_backingProperties.getFile(getPropertyName(), getDefaultValue());
    }

    @Override
    protected void setToStorage(File value) {
      m_backingProperties.setFile(getPropertyName(), value);
    }
  }

  private final class DirectoryProperty extends Property<Directory> {
    public DirectoryProperty(String propertyName) {
      super(propertyName, new Directory());
    }

    @Override
    protected Directory get() {
      final File f = m_backingProperties.getFile(getPropertyName(), null);

      if (f != null) {
        try {
          return new Directory(f);
        }
        catch (Directory.DirectoryException e) {
          // fall through.
        }
      }

      return new Directory();
    }

    @Override
    protected void setToStorage(Directory value) {
      m_backingProperties.setFile(getPropertyName(), value.getFile());
    }
  }

  private final class BooleanProperty extends Property<Boolean> {
    public BooleanProperty(String propertyName, boolean defaultValue) {
      super(propertyName, defaultValue);
    }

    @Override
    protected Boolean get() {
      return m_backingProperties.getBoolean(getPropertyName(),
                                            getDefaultValue());
    }

    @Override
    protected void setToStorage(Boolean value) {
      m_backingProperties.setBoolean(getPropertyName(), value);
    }
  }

  private final class RectangleProperty extends Property<Rectangle> {
    public RectangleProperty(String propertyName) {
      super(propertyName, null);
    }

    public Rectangle get() {
      final String property =
        m_backingProperties.getProperty(getPropertyName(), null);

      if (property != null) {
        final StringTokenizer tokenizer = new StringTokenizer(property, ",");

        try {
          return new Rectangle(Integer.parseInt(tokenizer.nextToken()),
                               Integer.parseInt(tokenizer.nextToken()),
                               Integer.parseInt(tokenizer.nextToken()),
                               Integer.parseInt(tokenizer.nextToken()));
        }
        catch (NoSuchElementException e) {
          // Ignore.
        }
        catch (NumberFormatException e) {
          // Ignore.
        }
      }

      return getDefaultValue();
    }

    public void setToStorage(Rectangle value) {
      m_backingProperties.setProperty(
        getPropertyName(),
        value.x + "," + value.y + "," + value.width + "," + value.height);
    }
  }
}
