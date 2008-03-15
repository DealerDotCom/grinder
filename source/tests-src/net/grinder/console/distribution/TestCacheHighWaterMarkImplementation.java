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

import net.grinder.console.distribution.CacheHighWaterMarkImplementation.CacheIdentity;
import net.grinder.messages.agent.CacheHighWaterMark;
import junit.framework.TestCase;


/**
 * Unit tests for {@link CacheHighWaterMarkImplementation}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestCacheHighWaterMarkImplementation extends TestCase {

  public void testCacheHighWaterMark() throws Exception {
    final CacheIdentity cache1 = new CacheIdentity() {};
    final CacheIdentity cache2 = new CacheIdentity() {};

    final CacheHighWaterMark a =
      new CacheHighWaterMarkImplementation(cache1, 100);
    final CacheHighWaterMark b =
      new CacheHighWaterMarkImplementation(cache1, 100);
    final CacheHighWaterMark c =
      new CacheHighWaterMarkImplementation(cache1, 120);
    final CacheHighWaterMark d =
      new CacheHighWaterMarkImplementation(cache2, 120);

    assertTrue(a.isSameOrAfter(b));
    assertTrue(a.isSameOrAfter(a));
    assertTrue(b.isSameOrAfter(a));
    assertFalse(a.isSameOrAfter(c));
    assertTrue(c.isSameOrAfter(a));
    assertFalse(d.isSameOrAfter(a));
    assertFalse(a.isSameOrAfter(d));
  }

}
