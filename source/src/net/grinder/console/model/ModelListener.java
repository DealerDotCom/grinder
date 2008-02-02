// Copyright (C) 2001 - 2008 Philip Aston
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

package net.grinder.console.model;

import java.util.EventListener;
import java.util.Set;


/**
 * Interface for listeners to {@link SampleModelImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public interface ModelListener extends EventListener {

  /**
   * Called when the model state has changed.
   */
  void stateChanged();

  /**
   * Called when the model has a new sample.
   */
  void newSample();

  /**
   * Called when new tests have been added to the model.
   *
   * @param newTests The new tests.
   * @param modelTestIndex New index structure for the model's tests.
   */
  void newTests(Set newTests, ModelTestIndex modelTestIndex);

  /**
   * Called when existing tests and statistics views should be
   * discarded.
   */
  void resetTests();
}
