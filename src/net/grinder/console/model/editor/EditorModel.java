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

import java.io.File;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.Resources;


/**
 * Editor model.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class EditorModel {

  private final Resources m_resources;
  private final TextSource.Factory m_textSourceFactory;
  private final Buffer m_defaultBuffer;

  /** Synchronise on m_listeners before accessing. */
  private final List m_listeners = new LinkedList();

  private final Map m_fileBuffers = new HashMap();

  private Buffer m_selectedBuffer;

  /**
   * Constructor.
   *
   * @param resources Resources.
   * @param textSourceFactory Factory for {@link TextSource}s.
   */
  public EditorModel(Resources resources,
                     TextSource.Factory textSourceFactory) {
    m_resources = resources;
    m_textSourceFactory = textSourceFactory;
    m_defaultBuffer = new Buffer(m_resources, m_textSourceFactory.create());
    addBufferListener(m_defaultBuffer);
  }

  /**
   * Get the currently active buffer.
   *
   * @return The active buffer.
   */
  public Buffer getSelectedBuffer() {
    return m_selectedBuffer;
  }

  /**
   * Select the default buffer.
   */
  public void selectDefaultBuffer() {
    selectBuffer(m_defaultBuffer);
  }

  /**
   * Select a new buffer.
   */
  public void selectNewBuffer() {
    final Buffer buffer =
      new Buffer(m_resources, m_textSourceFactory.create(), null);

    addBufferListener(buffer);

    selectBuffer(buffer);
  }

  /**
   * Select the buffer for the given file.
   *
   * @param file The file.
   * @return The buffer.
   * @throws ConsoleException If a buffer could not be selected for the file.
   */
  public Buffer selectBufferForFile(File file) throws ConsoleException {
    final Buffer existingBuffer = (Buffer)m_fileBuffers.get(file);
    final Buffer buffer;

    if (existingBuffer != null) {
      buffer = existingBuffer;
    }
    else {
      buffer = new Buffer(m_resources, m_textSourceFactory.create(), file);
      addBufferListener(buffer);

      buffer.load();

      m_fileBuffers.put(file, buffer);
    }

    selectBuffer(buffer);

    return buffer;
  }

  private void selectBuffer(Buffer buffer) {
    if (buffer != m_selectedBuffer) {
      final Buffer oldBuffer = m_selectedBuffer;

      m_selectedBuffer = buffer;

      if (oldBuffer != null) {
        fireBufferChanged(oldBuffer);
      }

      fireBufferChanged(buffer);
    }
  }

  private void addBufferListener(final Buffer buffer) {
    buffer.getTextSource().addListener(new TextSource.Listener() {
        public void textSourceChanged(boolean dirtyStateChanged) {
          if (dirtyStateChanged) {
            fireBufferChanged(buffer);
          }
        }
      });
  }

  private void fireBufferChanged(Buffer buffer) {
    synchronized (m_listeners) {
      final Iterator iterator = m_listeners.iterator();

      while (iterator.hasNext()) {
        final Listener listener = (Listener)iterator.next();
        listener.bufferChanged(buffer);
      }
    }
  }

  /**
   * Add a new listener.
   *
   * @param listener The listener.
   */
  public void addListener(Listener listener) {
    synchronized (m_listeners) {
      m_listeners.add(listener);
    }
  }

  /**
   * Interface for listeners.
   */
  public interface Listener extends EventListener {

    /**
     * Called when a buffer's state has changed - i.e. the buffer has
     * become dirty, or become clean, or has been selected, or has
     * been unselected.
     *
     * @param buffer The buffer.
     */
    void bufferChanged(Buffer buffer);
  }
}
