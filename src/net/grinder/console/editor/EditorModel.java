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

package net.grinder.console.editor;

import java.io.File;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.console.distribution.AgentCacheState;


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

  private final LinkedList m_bufferList = new LinkedList();
  private final Map m_fileBuffers = new HashMap();

  private int m_nextNewBufferNameIndex = 0;

  private Buffer m_selectedBuffer;
  private File m_markedScript;

  /**
   * Constructor.
   *
   * @param resources Resources.
   * @param textSourceFactory Factory for {@link TextSource}s.
   * @param agentCacheState The agent cache state.
   */
  public EditorModel(Resources resources,
                     TextSource.Factory textSourceFactory,
                     AgentCacheState agentCacheState) {
    m_resources = resources;
    m_textSourceFactory = textSourceFactory;
    m_defaultBuffer = new Buffer(m_resources,
                                 m_textSourceFactory.create(),
                                 createNewBufferName());
    addBuffer(m_defaultBuffer);

    m_defaultBuffer.getTextSource().setText(
      m_resources.getStringFromFile(
        "scriptSupportUnderConstruction.text", true));
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
    final Buffer buffer = new Buffer(m_resources,
                                     m_textSourceFactory.create(),
                                     createNewBufferName());
    addBuffer(buffer);

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
    final Buffer existingBuffer = getBufferForFile(file);
    final Buffer buffer;

    if (existingBuffer != null) {
      buffer = existingBuffer;
    }
    else {
      buffer = new Buffer(m_resources, m_textSourceFactory.create(), file);
      buffer.load();
      addBuffer(buffer);

      m_fileBuffers.put(file, buffer);
    }

    selectBuffer(buffer);

    return buffer;
  }

  /**
   * Get the buffer for the given file.
   *
   * @param file The file.
   * @return The buffer; <code>null</code> => there is no buffer for the file.
   */
  public Buffer getBufferForFile(File file) {
    return (Buffer)m_fileBuffers.get(file);
  }

  /**
   * Save buffer as another file.
   *
   * @param buffer The buffer.
   * @param file The file.
   * @throws ConsoleException If a buffer could not be saved as the file.
   */
  public void saveBufferAs(Buffer buffer, File file) throws ConsoleException {

    // Could redo this with EditorModel responding to a notification
    // from the Buffer.
    final File oldFile = buffer.getFile();

    // This will fire bufferChanged if the buffer becomes clean.
    buffer.save(file);

    if (!file.equals(oldFile)) {
      if (oldFile != null) {
        m_fileBuffers.remove(oldFile);
      }

      m_fileBuffers.put(file, buffer);

      // Fire that bufferChanged because it is associated with a new
      // file.
      fireBufferChanged(buffer);
    }
  }

  /**
   * Return a the current buffer list.
   *
   * @return The buffer list.
   */
  public Buffer[] getBuffers() {
    return (Buffer[])m_bufferList.toArray(new Buffer[m_bufferList.size()]);
  }

  /**
   * Select a buffer.
   *
   * @param buffer The buffer.
   */
  public void selectBuffer(Buffer buffer) {
    if (buffer == null || !buffer.equals(m_selectedBuffer)) {

      final Buffer oldBuffer = m_selectedBuffer;

      m_selectedBuffer = buffer;

      if (oldBuffer != null) {
        fireBufferChanged(oldBuffer);
      }

      if (buffer != null) {
        fireBufferChanged(buffer);
      }
    }
  }

  /**
   * Close a buffer.
   *
   * @param buffer The buffer.
   */
  public void closeBuffer(Buffer buffer) {
    if (m_bufferList.remove(buffer)) {
      final File file = buffer.getFile();

      if (buffer.equals(getBufferForFile(file))) {
        m_fileBuffers.remove(file);
      }

      if (buffer.equals(getSelectedBuffer())) {
        final int numberOfBuffers = m_bufferList.size();

        if (numberOfBuffers > 0) {
          selectBuffer((Buffer)m_bufferList.get(numberOfBuffers - 1));
        }
        else {
          selectBuffer(null);
        }
      }

      fireBufferRemoved(buffer);
    }
  }

  /**
   * Get the currently marked script.
   *
   * @return The active buffer.
   */
  public File getMarkedScript() {
    return m_markedScript;
  }

  /**
   * Get the currently marked script.
   *
   * @param markedScript The marked script.
   */
  public void setMarkedScript(File markedScript) {
    m_markedScript = markedScript;
  }

  private void addBuffer(final Buffer buffer) {
    buffer.getTextSource().addListener(new TextSource.Listener() {
        public void textSourceChanged(boolean dirtyStateChanged) {
          if (dirtyStateChanged) {
            fireBufferChanged(buffer);
          }
        }
      });

    m_bufferList.add(buffer);

    fireBufferAdded(buffer);
  }

  private void fireBufferAdded(Buffer buffer) {
    synchronized (m_listeners) {
      final Iterator iterator = m_listeners.iterator();

      while (iterator.hasNext()) {
        final Listener listener = (Listener)iterator.next();
        listener.bufferAdded(buffer);
      }
    }
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

  private void fireBufferRemoved(Buffer buffer) {
    synchronized (m_listeners) {
      final Iterator iterator = m_listeners.iterator();

      while (iterator.hasNext()) {
        final Listener listener = (Listener)iterator.next();
        listener.bufferRemoved(buffer);
      }
    }
  }

  private String createNewBufferName() {

    final String prefix = m_resources.getString("newBuffer.text");

    try {
      if (m_nextNewBufferNameIndex == 0) {
        return prefix;
      }
      else {
        return prefix + " " + m_nextNewBufferNameIndex;
      }
    }
    finally {
      ++m_nextNewBufferNameIndex;
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
   * Return whether the given file should be considered to be a Python
   * file. For now this is just based on name.
   *
   * @param f The file.
   * @return <code>true</code> => its a Python file.
   */
  public boolean isPythonFile(File f) {
    return f != null && f.getName().toLowerCase().endsWith(".py");
  }

  /**
   * Return whether the given file should be marked as boring.
   *
   * @param f The file.
   * @return a <code>true</code> => its boring.
   */
  public boolean isBoringFile(File f) {
    if (f == null) {
      return false;
    }

    final String name = f.getName().toLowerCase();

    return
      f.isHidden() ||
      name.endsWith(".class") ||
      name.startsWith("~") ||
      name.endsWith("~") ||
      name.startsWith("#") ||
      name.endsWith(".exe") ||
      name.endsWith(".gif") ||
      name.endsWith(".jpeg") ||
      name.endsWith(".jpg") ||
      name.endsWith(".tiff");
  }


  /**
   * Interface for listeners.
   */
  public interface Listener extends EventListener {

    /**
     * Called when a buffer has been added.
     *
     * @param buffer The buffer.
     */
    void bufferAdded(Buffer buffer);

    /**
     * Called when a buffer's state has changed - i.e. the buffer has
     * become dirty, or become clean, or has been selected, or has
     * been unselected, or has become associated with a new file.
     *
     * @param buffer The buffer.
     */
    void bufferChanged(Buffer buffer);

    /**
     * Called when a buffer has been removed.
     *
     * @param buffer The buffer.
     */
    void bufferRemoved(Buffer buffer);
  }
}
