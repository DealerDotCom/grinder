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
  private boolean m_supressBufferChangeNotification;

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
    m_defaultBuffer = createBuffer(null);
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
      buffer = createBuffer(file);

      // Listeners will be notified anyway because of the
      // buffer.setActive().
      m_supressBufferChangeNotification = true;

      try {
        buffer.load();
      }
      finally {
        m_supressBufferChangeNotification = false;
      }

      m_fileBuffers.put(file, buffer);
    }

    selectBuffer(buffer);

    return buffer;
  }

  private void selectBuffer(Buffer buffer) {
    if (m_selectedBuffer != buffer) {
      if (m_selectedBuffer != null) {
        m_selectedBuffer.setActive(false);
      }

      buffer.setActive(true);
      m_selectedBuffer = buffer;
    }
  }

  private Buffer createBuffer(File file) {
    final TextSource textSource = m_textSourceFactory.create();

    final Buffer buffer;
    
    if (file != null) {
      buffer = new Buffer(m_resources, textSource, file);
    }
    else {
      buffer = new Buffer(m_resources, textSource);
    }

    buffer.addListener(new Buffer.Listener() {
        public void bufferChanged() { fireBufferChanged(buffer); }
      });

    textSource.addListener(new TextSource.Listener() {
        public void textChanged(boolean firstEdit) {
          if (firstEdit) {
            fireBufferChanged(buffer);
          }
        }
      });

    return buffer;
  }

  private void fireBufferChanged(Buffer buffer) {

    if (!m_supressBufferChangeNotification) {
      synchronized (m_listeners) {
        final Iterator iterator = m_listeners.iterator();

        while (iterator.hasNext()) {
          final Listener listener = (Listener)iterator.next();
          listener.bufferChanged(buffer);
        }
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
     * Called when a buffer has been activated.
     *
     * @param buffer The buffer.
     */
    void bufferChanged(Buffer buffer);
  }
}
