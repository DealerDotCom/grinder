// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

package net.grinder;

import java.io.File;
import java.io.PrintWriter;

import net.grinder.common.Logger;
import net.grinder.engine.agent.Agent;
import net.grinder.util.SimpleLogger;


/**
 * This is the entry point of The Grinder agent process.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public final class Grinder {

  private Grinder() {
  }

  /**
   * The Grinder agent process entry point.
   *
   * @param args Command line arguments.
   * @exception Exception If an error occurred.
   */
  public static void main(String[] args) throws Exception {

    if (args.length > 1) {
      System.err.println("Usage: java " + Grinder.class.getName() +
                         " [alternatePropertiesFilename]");
      System.exit(1);
    }

    final Logger logger = new SimpleLogger("agent",
                                           new PrintWriter(System.out),
                                           new PrintWriter(System.err));

    final Agent agent =
      new Agent(logger, args.length == 1 ? new File(args[0]) : null);

    agent.run();

    agent.shutdown();
  }
}
