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

  private Buffer m_activeBuffer;

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

    final TextSource textSource = m_textSourceFactory.create();
    m_defaultBuffer = new Buffer(m_resources, textSource);
    textSource.addListener(new TextSourceListener(m_defaultBuffer));
  }

  /**
   * Select the default buffer.
   */
  public void selectDefaultBuffer() {
    setActiveBuffer(m_defaultBuffer);
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
      final TextSource textSource = m_textSourceFactory.create();
      buffer = new Buffer(m_resources, textSource, file);
      textSource.addListener(new TextSourceListener(buffer));

      buffer.load();

      m_fileBuffers.put(file, buffer);
    }

    setActiveBuffer(buffer);

    return buffer;
  }

  private void setActiveBuffer(Buffer buffer) {
    if (m_activeBuffer != buffer) {
      if (m_activeBuffer != null) {
        m_activeBuffer.setActive(false);
      }

      buffer.setActive(true);
      m_activeBuffer = buffer;

      synchronized (m_listeners) {
        final Iterator iterator = m_listeners.iterator();

        while (iterator.hasNext()) {
          final Listener listener = (Listener)iterator.next();
          listener.bufferActivated(buffer);
        }
      }
    }
  }

  private class TextSourceListener implements TextSource.Listener {
    private final Buffer m_buffer;

    public TextSourceListener(Buffer buffer) {
      m_buffer = buffer;
    }

    public void textChanged() {
      synchronized (m_listeners) {
        final Iterator iterator = m_listeners.iterator();

        while (iterator.hasNext()) {
          final Listener listener = (Listener)iterator.next();
          listener.bufferChanged(m_buffer);
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
    void bufferActivated(Buffer buffer);

    /**
     * Called when a buffer has been changed.
     *
     * @param buffer The buffer.
     */
    void bufferChanged(Buffer buffer);
  }
}
