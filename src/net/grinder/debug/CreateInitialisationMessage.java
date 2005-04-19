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

package net.grinder.debug;

import java.io.File;

import net.grinder.communication.StreamSender;
import net.grinder.engine.messages.InitialiseGrinderMessage;


/**
 * Utility to create a serialised InitialiseGrinderMessage. This can
 * be used to debug a stand alone worker process. The message is
 * output to stdout.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class CreateInitialisationMessage {

  /**
   * Entry point. The process takes a single argument - the name of the script
   * file to run. The message is output to stdout.
   *
   * @param arguments
   *          The arguments.
   * @exception Exception
   *              if an error occurs
   */
  public static void main(String[] arguments) throws Exception {

    if (arguments.length != 1) {
      System.err.println("Usage: java " +
          CreateInitialisationMessage.class.getName() +
          " scriptFile > message");
      System.exit(-1);
    }

    final File scriptFile = new File(arguments[0]);

    final InitialiseGrinderMessage initialisationMessage =
      new InitialiseGrinderMessage("agentID",
                                   "processID",
                                   false,
                                   scriptFile,
                                   scriptFile.getAbsoluteFile()
                                     .getParentFile());

    new StreamSender(System.out).send(initialisationMessage);
  }

  /**
   * Shut up Checkstyle.
   */
  private CreateInitialisationMessage() {
  }
}
