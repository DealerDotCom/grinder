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

package net.grinder.console.distribution;

import java.io.Serializable;

import net.grinder.messages.agent.CacheHighWaterMark;


/**
 * Implementation of {@link CacheHighWaterMark}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
final class CacheHighWaterMarkImplementation
  implements CacheHighWaterMark, Serializable {

  private static final long serialVersionUID = 1L;

  private final CacheIdentity m_cacheIdentity;
  private final long m_highWaterMark;

  public CacheHighWaterMarkImplementation(CacheIdentity cacheIdentity,
                                          long highWaterMark) {
    m_cacheIdentity = cacheIdentity;
    m_highWaterMark = highWaterMark;
  }

  public boolean isSameOrAfter(CacheHighWaterMark other) {
    // For now, we only support comparison with other
    // CacheHighWaterMarkImplementations.
    final CacheHighWaterMarkImplementation otherHighWater =
      (CacheHighWaterMarkImplementation)other;

    return m_cacheIdentity.equals(otherHighWater.m_cacheIdentity) &&
           m_highWaterMark >= otherHighWater.m_highWaterMark;
  }

  /**
   * Opaque object representing the cache parameters.
   */
  interface CacheIdentity extends Serializable {
  }
}
