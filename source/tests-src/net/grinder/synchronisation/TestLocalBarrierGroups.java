// Copyright (C) 2011 Philip Aston
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

package net.grinder.synchronisation;

import static net.grinder.testutility.AssertUtilities.assertNotEquals;
import static net.grinder.testutility.Serializer.serialize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentity;
import net.grinder.synchronisation.BarrierGroup.BarrierIdentityGenerator;

import org.junit.Test;


/**
 * Unit tests for {@link LocalBarrierGroups}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestLocalBarrierGroups {

  @Test public void testCreateAndRetrieve() {
    final BarrierGroups groups = new LocalBarrierGroups();

    final BarrierGroup a = groups.getGroup("A");
    assertSame(a, groups.getGroup("A"));
    assertNotSame(a, groups.getGroup("B"));
  }

  @Test public void testIdentityGeneration() {
    final BarrierIdentityGenerator generator =
      new LocalBarrierGroups().getIdentityGenerator();

    final BarrierIdentity one = generator.next();
    final BarrierIdentity two = generator.next();

    assertNotEquals(one, two);
  }

  @Test public void testIdentityIsSerializable() throws Exception {
    final BarrierIdentityGenerator generator =
      new LocalBarrierGroups().getIdentityGenerator();

    final BarrierIdentity id = generator.next();

    final BarrierIdentity serializedID = serialize(id);

    assertEquals(id, serializedID);
  }

  @Test public void testIdentityEquality() throws Exception {
    final BarrierIdentityGenerator generator =
      new LocalBarrierGroups().getIdentityGenerator();

    final BarrierIdentity one = generator.next();
    final BarrierIdentity two = generator.next();

    assertEquals(one, one);
    assertNotEquals(one, two);
    assertNotEquals(one, this);
    assertNotEquals(one, null);
  }
}
