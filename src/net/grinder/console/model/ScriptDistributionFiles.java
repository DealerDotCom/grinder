// Copyright (C) 2003 Philip Aston
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

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;


/**
 * Class encapsulating the script distribution files.
 *
 * Not thread safe.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ScriptDistributionFiles implements Serializable {

  private static final String ROOT_DIRECTORY_PROPERTY = "rootDirectory";
  private static final String SCRIPT_FILE_PROPERTY = "scriptFile";
  private static final String ADDITIONAL_FILES_PROPERTY = "additionalFiles";

  private final String m_propertyPrefix;
  private File m_rootDirectory;
  private File m_scriptFile;
  private File[] m_additionalFiles;

  private int m_hashCode;

  ScriptDistributionFiles(String propertyPrefix,
			  GrinderProperties properties) {

    m_propertyPrefix = propertyPrefix;

    final GrinderProperties ourProperties =
      properties.getPropertySubset(propertyPrefix);

    setRootDirectory(
      new File(ourProperties.getProperty(ROOT_DIRECTORY_PROPERTY, ".")));

    final GrinderProperties additionalFileProperties =
      ourProperties.getPropertySubset(ADDITIONAL_FILES_PROPERTY);

    final Set additionalFiles = new HashSet(additionalFileProperties.size());

    final Iterator iterator = additionalFileProperties.values().iterator();
    
    while (iterator.hasNext()) {
      additionalFiles.add(new File((String)iterator.next()));
    }

    final String scriptFileString =
      ourProperties.getProperty(SCRIPT_FILE_PROPERTY, null);

    setFiles(scriptFileString != null ? new File(scriptFileString) : null,
	     additionalFiles);
  }

  final void addToProperties(GrinderProperties properties) {
    properties.setProperty(m_propertyPrefix + ROOT_DIRECTORY_PROPERTY,
			   getRootDirectory().getPath());

    final File scriptFile = getScriptFile();

    if (scriptFile != null) {
      properties.setProperty(m_propertyPrefix + SCRIPT_FILE_PROPERTY,
			     scriptFile.getPath());
    }

    for (int i=0; i<m_additionalFiles.length; ++i) {
      properties.setProperty(
	m_propertyPrefix + ADDITIONAL_FILES_PROPERTY + "." + i,
	m_additionalFiles[i].getPath());
    }
  }

  /**
   * Get the root directory for this file set.
   *
   * @return The root directory.
   */
  public final File getRootDirectory() {
    return m_rootDirectory;
  }

  /**
   * Set the root directory for this file set.
   *
   * @param directory The root directory.
   */
  public final void setRootDirectory(File directory) {
    m_rootDirectory = directory;
    updateHashCode();
  }

  /**
   * Return the current script file, or <code>null</code> if none has
   * been chosen.
   *
   * @return The script file.
   */
  public final File getScriptFile() {
    return m_scriptFile;
  }

  /**
   * Return the additional files that make up the distribution.
   *
   * @return The additional files.
   */
  public final File[] getAdditionalFiles() {
    return m_additionalFiles;
  }

  /**
   * Set the file set.
   *
   * @param scriptFile The script file.
   * @param additionalFiles Other files that should be distributed
   * with the script.
   */
  public final void setFiles(File scriptFile, Collection additionalFiles) {

    m_scriptFile = scriptFile;
    
    // Sort to convert to canonical form.
    final Set sortedAdditionalFiles = new TreeSet(additionalFiles);
    sortedAdditionalFiles.remove(scriptFile);

    m_additionalFiles = (File[])sortedAdditionalFiles.toArray(new File[0]);
    
    updateHashCode();
  }

  /**
   * Implement equality.
   *
   * @param o Object to compare.
   * @return <code>true</code> => its equal.
   */
  public final boolean equals(Object o) {

    if (!(o instanceof ScriptDistributionFiles)) {
      return false;
    }

    if (o.hashCode() != hashCode()) {
      return false;
    }

    final ScriptDistributionFiles other = (ScriptDistributionFiles)o;

    if (!other.getRootDirectory().equals(getRootDirectory())) {
      return false;
    }

    final File scriptFile = getScriptFile();
    final File otherScriptFile = other.getScriptFile();
    
    if (scriptFile == null && otherScriptFile != null ||
	scriptFile != null && otherScriptFile == null) {
      return false;
    }

    if (scriptFile != null && !scriptFile.equals(otherScriptFile)) {
      return false;
    }

    final File[] additionalFiles = getAdditionalFiles();
    final File[] otherAdditionalFiles = other.getAdditionalFiles();

    if (additionalFiles.length != otherAdditionalFiles.length) {
      return false;
    }

    for (int i=0; i<additionalFiles.length; ++i) {
      if (!additionalFiles[i].equals(otherAdditionalFiles[i])) {
	return false;
      }
    }

    return true;
  }

  private final void updateHashCode() {
    m_hashCode = getRootDirectory().hashCode();

    final File scriptFile = getScriptFile();

    if (scriptFile != null) {
      m_hashCode ^= scriptFile.hashCode();
    }
    
    final File[] additionalFiles = getAdditionalFiles();

    if (additionalFiles != null) {
      for (int i=0; i<additionalFiles.length; ++i) {
	m_hashCode ^= additionalFiles[i].hashCode();
      }
    }
  }

  /**
   * Return a hash code.
   *
   * @return The hash code.
   */
  public final int hashCode() {
    return m_hashCode;
  }

  public final String toString() {
    final StringBuffer result = new StringBuffer();

    result.append("ScriptDistributionFiles('");
    result.append(getRootDirectory());
    result.append("', '");
    result.append(getScriptFile());
    result.append("', '");

    final File[] additionalFiles = getAdditionalFiles();

    for (int i=0; i<additionalFiles.length; ++i) {

      if (i != 0) {
	result.append(", ");
      }

      result.append(additionalFiles[i]);
    }

    result.append("')");

    return result.toString();
  }
}
