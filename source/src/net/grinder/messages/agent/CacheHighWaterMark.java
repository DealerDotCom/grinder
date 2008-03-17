// Copyright (C) 2008 Philip Aston
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

package net.grinder.messages.agent;

import java.io.Serializable;


/**
 * A checkpoint of the agent cache state. The implementation is opaque to
 * agents.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public interface CacheHighWaterMark extends Serializable {

  /**
   * Compare this cache state with another.
   *
   * <p>
   * We don't use {@link Comparable} since this is not a strict ordering. Two
   * <code>CacheHighWaterMark</code>s for different caches (perhaps the key
   * information about the cache has changed) <code>x</code> and
   * <code>y</code>, will return <code>true</code> for both
   * <code>x.isLater(y)</code> and <code>y.isLater(x)</code>.
   * </p>
   *
   *
   * @param other
   *            The state to compare.
   * @return <code>true</code> if this cache state is for the same cache and
   *         is later than <code>other</code>, or this cache state is for a
   *         different cache, or <code>other</code> is <code>null</code>.
   */
  boolean isLater(CacheHighWaterMark other);
}