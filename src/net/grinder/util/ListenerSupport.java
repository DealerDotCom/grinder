// Copyright (C) 2005 Philip Aston
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

package net.grinder.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Generic support for listeners.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class ListenerSupport {

  private final List m_listeners = new LinkedList();

  /**
   * Marker interface for listeners.
   */
  public interface Listener {
  }

  /**
   * Simple inform interface.
   */
  public interface Informer {
    /**
     * Inform the listeners that something has happened.
     */
    void informListeners();
  }

  /**
   * Add a listener.
   *
   * @param listener The listener.
   */
  public void add(Listener listener) {
    synchronized (m_listeners) {
      m_listeners.add(listener);
    }
  }

  /**
   * Abstract class for notifying the listeners. Should be subclassed
   * to perform the appropriate listener invocation in {@link
   * #inform}.
   *
   * <p>If the subclass needs to pass additional state to the
   * listener, it can do so using member state. It can synchronise on
   * the result of {@link #getMutex} whilst doing so. For example:</p>
   *
   * <code>
   *   private Foo m_foo;
   *
   *   public void informOfFoo(Foo foo) {
   *     synchronized (getMutex()) {
   *       m_foo = foo;
   *       informListeners();
   *     }
   *   }
   *
   *  protected void inform(Listener listener) {
   *    ((MyListener)listener).informOfFoo(m_foo);
   *  }
   * </code>
   */
  public abstract class AbstractInformer implements Informer {

    /**
     * Call to notify the listeners.
     */
    public void informListeners() {
      synchronized (m_listeners) {
        final Iterator iterator = m_listeners.iterator();

        while (iterator.hasNext()) {
          inform((Listener)iterator.next());
        }
      }
    }

    /**
     * Subclasses should implement this to invoke the listener appropriately.
     *
     * @param listener The listener to be notifed.
     */
    protected abstract void inform(Listener listener);

    /**
     * An object the subclass can use to protect member state it is
     * tunnelling throught to {@link #inform}.
     *
     * @return A suitable lock object.
     */
    protected final Object getMutex() {
      return m_listeners;
    }
  }
}
