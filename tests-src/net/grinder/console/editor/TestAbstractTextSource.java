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

package net.grinder.console.editor;

import junit.framework.TestCase;

import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test for {@link AbstractTextSource}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestAbstractTextSource extends TestCase {

  public void testAbstractTextSource() throws Exception {
    final AbstractTextSource textSource = new StringTextSource();

    assertTrue(textSource.isDirty());

    textSource.setChanged();

    assertTrue(textSource.isDirty());

    final RandomStubFactory listener1StubFactory =
      new RandomStubFactory(TextSource.Listener.class);
    final TextSource.Listener listener1 =
      (TextSource.Listener)listener1StubFactory.getStub();

    final RandomStubFactory listener2StubFactory =
      new RandomStubFactory(TextSource.Listener.class);
    final TextSource.Listener listener2 =
      (TextSource.Listener)listener2StubFactory.getStub();

    textSource.addListener(listener1);
    textSource.addListener(listener2);

    textSource.setChanged();

    assertTrue(textSource.isDirty());
    listener1StubFactory.assertSuccess("textSourceChanged", Boolean.FALSE);
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("textSourceChanged", Boolean.FALSE);
    listener2StubFactory.assertNoMoreCalls();

    textSource.setClean();

    assertTrue(!textSource.isDirty());
    listener1StubFactory.assertSuccess("textSourceChanged", Boolean.TRUE);
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("textSourceChanged", Boolean.TRUE);
    listener2StubFactory.assertNoMoreCalls();

    textSource.setClean();

    assertTrue(!textSource.isDirty());
    listener1StubFactory.assertSuccess("textSourceChanged", Boolean.FALSE);
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("textSourceChanged", Boolean.FALSE);
    listener2StubFactory.assertNoMoreCalls();

    textSource.setChanged();

    assertTrue(textSource.isDirty());
    listener1StubFactory.assertSuccess("textSourceChanged", Boolean.TRUE);
    listener1StubFactory.assertNoMoreCalls();
    listener2StubFactory.assertSuccess("textSourceChanged", Boolean.TRUE);
    listener2StubFactory.assertNoMoreCalls();
  }
}
