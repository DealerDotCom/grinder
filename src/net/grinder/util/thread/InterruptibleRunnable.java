package net.grinder.util.thread;


/**
 * Marker interface indicating a {@link Runnable} that can also be interrupted.
 *
 * <p>
 * {@link InterruptedException} is too easy to ignore. {@link Runnable}s that
 * implement this also guarantee they will cleanly exit {@link Runnable#run()}
 * if their thread is interrupted.
 * </p>
 *
 * @author Philip Aston
 * @version $Revision$
 * @see UncheckedInterruptedException
 */
public interface InterruptibleRunnable extends java.lang.Runnable {
}
