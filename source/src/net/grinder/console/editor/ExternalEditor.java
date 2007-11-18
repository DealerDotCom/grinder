package net.grinder.console.editor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.grinder.console.distribution.AgentCacheState;

/**
 * Handles opening a file in external editor.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
class ExternalEditor {

  private static final ThreadGroup s_threadGroup =
    new ThreadGroup("ExternalEditor");

  static final ThreadGroup getThreadGroup() {
    return s_threadGroup;
  }

  private final AgentCacheState m_agentCacheState;
  private final String m_command;
  private final String m_arguments;


  public ExternalEditor(AgentCacheState agentCacheState,
                        String command,
                        String arguments) {
    m_agentCacheState = agentCacheState;
    m_command = command;
    m_arguments = arguments;
  }


  String[] fileToCommandLine(File file) {
    final List result = new ArrayList();
    result.add(m_command);

    boolean fileTemplateFound = false;

    if (m_arguments != null) {
      final StringTokenizer tokenizer = new StringTokenizer(m_arguments);

      while (tokenizer.hasMoreElements()) {
        final String token = tokenizer.nextToken();

        final String argument = token.replaceAll("%f", file.getAbsolutePath());
        result.add(argument);

        fileTemplateFound |= !argument.equals(token);
      }
    }

    if (!fileTemplateFound) {
      result.add(file.getAbsolutePath());
    }

    return (String[]) result.toArray(new String[result.size()]);
  }

  public void open(final File file) throws IOException {
    final long originalModificationTime = file.lastModified();

    final Process exec = Runtime.getRuntime().exec(
      fileToCommandLine(file),
      null,
      file.getParentFile());

    final Runnable handleCompletion = new Runnable() {
      public void run() {
        try {
          exec.waitFor();
        }
        catch (InterruptedException e) {
          // This thread has been interrupted, silently exit.
          return;
        }

        // If file no longer exists, lastModified will be 0.
        final long lastModified = file.lastModified();

        if (lastModified > originalModificationTime) {
          m_agentCacheState.setOutOfDate(lastModified);
        }
      }
    };

    final Thread thread = new Thread(getThreadGroup(),
                                     handleCompletion,
                                     "External edit of " + file);
    thread.setDaemon(true);
    thread.start();
  }
}