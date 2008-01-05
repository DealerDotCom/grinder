// Copyright (C) 2007 Philip Aston
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
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.communication;

import net.grinder.engine.messages.ClearCacheMessage;
import net.grinder.engine.messages.DistributeFileMessage;
import net.grinder.util.FileContents;


/**
 * Implementation of {@link DistributionControl}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class DistributionControlImplementation
  implements DistributionControl {

  private final ConsoleCommunication m_consoleCommunication;

  /**
   * Constructor.
   *
   * @param consoleCommunication
   *          The console communication handler.
   */
  public DistributionControlImplementation(
    ConsoleCommunication consoleCommunication) {
      m_consoleCommunication = consoleCommunication;
  }

  /**
   * Signal the agent processes to clear their file caches.
   */
  public void clearFileCaches() {
    m_consoleCommunication.sendToAgents(new ClearCacheMessage());
  }

  /**
   * Send a file to the file caches.
   *
   * @param fileContents The file contents.
   */
  public void sendFile(FileContents fileContents) {
    m_consoleCommunication.sendToAgents(
      new DistributeFileMessage(fileContents));
  }
}
