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

package net.grinder.engine.agent;

import java.io.File;
import java.io.IOException;

import net.grinder.common.Logger;
import net.grinder.engine.common.EngineException;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.communication.Sender;
import net.grinder.engine.messages.ClearCacheMessage;
import net.grinder.engine.messages.DistributeFileMessage;
import net.grinder.util.Directory;
import net.grinder.util.FileContents;


/**
 * Process {@link ClearCacheMessage}s and {@link
 * DistributeFileMessage}s received from the console.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class FileStore {

  /** Should make this a Directory one day. */
  private final File m_directory;
  private final Logger m_logger;

  public FileStore(File directory, Logger logger) throws FileStoreException {

    m_directory = directory.getAbsoluteFile();
    m_logger = logger;

    if (m_directory.exists()) {
      if (!m_directory.isDirectory()) {
        throw new FileStoreException(
          "Can't write to directory '" + m_directory +
          "' as file with that name already exists");
      }
    }
    else {
      if (!m_directory.mkdir()) {
        throw new FileStoreException(
          "Can't create directory '" + m_directory + "'");
      }
    }

    if (!m_directory.canWrite()) {
      throw new FileStoreException(
        "Can't write to directory '" + m_directory + "'");
    }

    try {
      new Directory(m_directory).deleteContents();
    }
    catch (IOException e) {
      throw new FileStoreException(
        "Can't delete contents of directory '" + m_directory + "'", e);
    }
  }

  public File getDirectory() {
    return m_directory;
  }

  public Sender getSender(final Sender delegate) {

    return new Sender() {
        public void send(Message message) throws CommunicationException {
          if (message instanceof ClearCacheMessage) {
            m_logger.output("Clearing file store");
            try {
              new Directory(m_directory).deleteContents();
            }
            catch (Directory.DirectoryException e) {
              m_logger.error("Could not clear file store: " + e.getMessage());

              // Throwing an exception causes the agent to silently
              // exit.
              throw new CommunicationException(e.getMessage(), e);
            }
          }
          else if (message instanceof DistributeFileMessage) {
            final FileContents fileContents =
              ((DistributeFileMessage)message).getFileContents();

            m_logger.output("Updating file store: " + fileContents);

            try {
              fileContents.create(m_directory);
            }
            catch (FileContents.FileContentsException e) {
              m_logger.error("Could not write file: " + e.getMessage());

              // Throwing an exception causes the agent to silently
              // exit.
              throw new CommunicationException(e.getMessage(), e);
            }
          }
          else {
            delegate.send(message);
          }
        }

        public void shutdown() {
          delegate.shutdown();
        }
      };
  }

  /**
   * Exception that indicates a <code>FileStore</code> related
   * problem.
   */
  public static final class FileStoreException extends EngineException {
    FileStoreException(String message) {
      super(message);
    }

    FileStoreException(String message, Throwable e) {
      super(message, e);
    }
  }
}
