// Copyright (C) 2007 Philip Aston
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

package net.grinder.engine.common;

import java.io.File;
import java.io.Serializable;


/**
 * Pairing of a script file and its root directory. The directory is not
 * necessarily the immediate parent of the file.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public final class ScriptLocation implements Serializable {
  private final File m_directory;
  private final File m_file;

  /**
   * Constructor for ScriptLocation.
   *
   * @param directory Script root directory.
   * @param file The script file.
   */
  public ScriptLocation(File directory, File file) {
    m_directory = directory;
    m_file = file;
  }

  /**
   * Accessor for the script root directory.
   *
   * @return The directory.
   */
  public File getDirectory() {
    return m_directory;
  }

  /**
   * Accessor for the script file.
   *
   * @return The file.
   */
  public File getFile() {
    return m_file;
  }

  /**
   * String representation.
   *
   * @return The string.
   */
  public String toString() {
    return m_file.getPath();
  }

  /**
   * Hash code.
   *
   * @return The hash code.
   */
  public int hashCode() {
    return getDirectory().hashCode() ^ getFile().hashCode();
  }

  /**
   * Equality.
   *
   * @param other Object to compare.
   * @return <code>true</code> if and only if we're equal to <code>other</code>.
   */
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof ScriptLocation)) {
      return false;
    }

    final ScriptLocation otherScriptLocation = (ScriptLocation)other;

    return getDirectory().equals(otherScriptLocation.getDirectory()) &&
           getFile().equals(otherScriptLocation.getFile());
  }
}
