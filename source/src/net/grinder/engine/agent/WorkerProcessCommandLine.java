// Copyright (C) 2004 - 2011 Philip Aston
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
 */
final class WorkerProcessCommandLine {

  private static final String AGENT_JAR_FILENAME = "grinder-agent.jar";

  private final List<String> m_command;
  private final int m_commandClassIndex;

  public WorkerProcessCommandLine(GrinderProperties properties,
                                  Properties systemProperties,
                                  String jvmArguments) {

    m_command = new ArrayList<String>();
    m_command.add(properties.getProperty("grinder.jvm", "java"));

    final String systemClasspath =
      systemProperties.getProperty("java.class.path");

    if (systemClasspath != null) {
      final File agent = findAgentJarFile(systemClasspath);

      if (agent != null) {
        m_command.add("-javaagent:" + agent.getAbsolutePath());
      }
    }

    if (jvmArguments != null) {
      // Really should allow whitespace to be escaped/quoted.
      final StringTokenizer tokenizer = new StringTokenizer(jvmArguments);

      while (tokenizer.hasMoreTokens()) {
        final String token = tokenizer.nextToken();

        m_command.add(token);
      }
    }

    final String additionalClasspath =
      properties.getProperty("grinder.jvm.classpath", null);

    final StringBuilder classpath = new StringBuilder();

    if (additionalClasspath != null) {
      classpath.append(additionalClasspath);
    }

    if (systemClasspath != null) {
      if (classpath.length() > 0) {
        classpath.append(File.pathSeparatorChar);
      }

      classpath.append(systemClasspath);
    }

    if (classpath.length() > 0) {
      m_command.add("-classpath");
      m_command.add(classpath.toString());
    }

    m_commandClassIndex = m_command.size();
    m_command.add(WorkerProcessEntryPoint.class.getName());
  }

  /**
   * Package scope for the unit tests.
   */
  public List<String> getCommandList() {
    return m_command;
  }

  private static final Set<String> s_unquoted = new HashSet<String>() { {
      add("-classpath");
      add("-client");
      add("-cp");
      add("-jar");
      add("-server");
    } };

  public String toString() {
    final String[] commandArray = getCommandList().toArray(new String[0]);

    final StringBuilder buffer = new StringBuilder(commandArray.length * 10);

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

  /**
   * Package scope for unit tests.
   *
   * @param path The path to search.
   */
  static File findAgentJarFile(String path) {

    for (String pathEntry : path.split(File.pathSeparator)) {
      final File siblingFile =
        new File(new File(pathEntry).getParent(), AGENT_JAR_FILENAME);

      if (siblingFile.exists()) {
        return siblingFile;
      }
    }

    return null;
  }
}
