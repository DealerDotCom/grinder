// Copyright (C) 2004 Philip Aston
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

package net.grinder.console.model.editor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Base {@link TextSource} implementation that adds {@link
 * TextSource.Listener} and {@link #isDirty} support. See {@link
 * #setClean} for important subclass responsibilities.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public abstract class AbstractTextSource implements TextSource {

  private boolean m_dirty;

  /** Synchronise on m_listeners before accessing. */
  private final List m_listeners = new LinkedList();

  /**
   * Return whether the text has changed since the last call to {@link
   * #getText} or {@link #setText}.
   *
   * @return <code>true</code> => the text has changed.
   */
  public boolean isDirty() {
    return m_dirty;
  }

  /**
   * Used by subclasses to mark that the <code>TextSource</code> is
   * clean. Subclasses should call <code>setClean()</code> in their
   * {@link TextSource.setText} and {@link TextSource.getText}
   * implementations.
   *
   */
  protected final void setClean() {
    final boolean oldDirty = m_dirty;
    m_dirty = false;
    fireTextSourceChanged(oldDirty);
  }


  /**
   * Used by subclasses to mark that the <code>TextSource</code> has
   * changed.
   */
  protected final void setChanged() {
    final boolean oldDirty = m_dirty;
    m_dirty = true;
    fireTextSourceChanged(!oldDirty);
  }

    /**
   * Listener registration.
   *
   * @param listener The listener.
   */
  public void addListener(Listener listener) {
    synchronized (m_listeners) {
      m_listeners.add(listener);
    }
  }

  private void fireTextSourceChanged(boolean firstEdit) {
    synchronized (m_listeners) {
      final Iterator iterator = m_listeners.iterator();

      while (iterator.hasNext()) {
        final Listener listener = (Listener)iterator.next();
        listener.textSourceChanged(firstEdit);
      }
    }
  }
}
