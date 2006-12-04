// Copyright (C) 2004, 2005 Philip Aston
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

package net.grinder.engine.agent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import net.grinder.common.GrinderProperties;
import net.grinder.engine.process.WorkerProcessEntryPoint;


/**
 * Builds the worker process command line.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class WorkerProcessCommandLine {

  private final List m_command;
  private final int m_commandClassIndex;

  public WorkerProcessCommandLine(GrinderProperties properties,
                                  Properties systemProperties,
                                  String jvmArguments) {

    m_command = new ArrayList();
    m_command.add(properties.getProperty("grinder.jvm", "java"));

    if (jvmArguments != null) {
      // Really should allow whitespace to be escaped/quoted.
      final StringTokenizer tokenizer = new StringTokenizer(jvmArguments);

      while (tokenizer.hasMoreTokens()) {
        m_command.add(tokenizer.nextToken());
      }
    }

    final String additionalClasspath =
      properties.getProperty("grinder.jvm.classpath", null);

    final String systemClasspath =
      systemProperties.getProperty("java.class.path");

    final StringBuffer classpath = new StringBuffer();

    if (additionalClasspath != null) {
      classpath.append(additionalClasspath);
    }

    if (additionalClasspath != null && systemClasspath != null) {
      classpath.append(File.pathSeparatorChar);
    }

    if (systemClasspath != null) {
      classpath.append(systemClasspath);
    }

    if (classpath.length() > 0) {
      m_command.add("-classpath");
      m_command.add(classpath.toString());
    }

    m_commandClassIndex = m_command.size();
    m_command.add(WorkerProcessEntryPoint.class.getName());
  }

  public String[] getCommandArray() {
    return (String[])m_command.toArray(new String[0]);
  }

  /**
   * Package scope for the unit tests.
   */
  List getCommandList() {
    return m_command;
  }

  private static final Set s_unquoted = new HashSet() { {
    add("-classpath");
    add("-client");
    add("-cp");
    add("-jar");
    add("-server");
  } };

  public String toString() {
    final String[] commandArray = getCommandArray();

    final StringBuffer buffer = new StringBuffer(commandArray.length * 10);

    for (int j = 0; j < commandArray.length; ++j) {
      if (j != 0) {
        buffer.append(" ");
      }

      final boolean shouldQuote =
        j != 0 &&
        j != m_commandClassIndex &&
        !s_unquoted.contains(commandArray[j]);

      if (shouldQuote) {
        buffer.append("'");
      }

      buffer.append(commandArray[j]);

      if (shouldQuote) {
        buffer.append("'");
      }
    }

    return buffer.toString();
  }
}
