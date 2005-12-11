package net.grinder.util.thread;


/**
 * Abstract implementation of {@link InterruptibleRunnable} that adapts to
 * {@link Runnable}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public abstract class AbstractInterruptibleRunnable
  implements InterruptibleRunnable, Runnable {

  /**
   * Implement {@link Runnable}.
   */
  public void run() {
    try {
      interruptibleRun();
    }
    catch (UncheckedInterruptedException e) {
      // Ignore.
    }
  }
}
