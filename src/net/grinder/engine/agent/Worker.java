// Copyright (C) 2005 Philip Aston
// All rights reserved.
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

import java.io.OutputStream;

import net.grinder.engine.common.EngineException;


/**
 * A worker.
 *
 * @author Philip Aston
 * @version $Revision$
 */
interface Worker {

  /**
   * Return the worker name.
   *
   * @return The name.
   */
  String getName();

  /**
   * Return an output stream connected to the input stream for the worker.
   *
   * @return The stream.
   */
  OutputStream getCommunicationStream();

  /**
   * Wait until the worker has completed. Return the exit status.
   *
   * @return See {@link net.grinder.engine.process.GrinderProcess} for
   * valid values.
   * @throws EngineException If an error occurs.
   * @throws InterruptedException If this thread is interrupted whilst
   * waiting.
   */
  int waitFor() throws InterruptedException, EngineException;

  /**
   * Destroy the worker.
   */
  void destroy();
}
