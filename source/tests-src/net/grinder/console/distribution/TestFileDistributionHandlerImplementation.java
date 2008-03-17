// Copyright (C) 2005 - 2008 Philip Aston
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

import java.io.File;

import net.grinder.console.communication.DistributionControl;
import net.grinder.console.distribution.CacheHighWaterMarkImplementation.CacheIdentity;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.FileContents;


/**
 * Unit test for {@link FileDistributionHandlerImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestFileDistributionHandlerImplementation
  extends AbstractFileTestCase {

  public void testFileDistributionHandlerImplementation() throws Exception {

    final RandomStubFactory distributionControlStubFactory =
      new RandomStubFactory(DistributionControl.class);
    final DistributionControl distributionControl =
      (DistributionControl)distributionControlStubFactory.getStub();

    final RandomStubFactory updateableAgentCacheStateStubFactory =
      new RandomStubFactory(UpdateableAgentCacheState.class);
    final UpdateableAgentCacheState updateableAgentCacheState =
      (UpdateableAgentCacheState)updateableAgentCacheStateStubFactory.getStub();

    final File[] files = {
      new File("a"),
      new File("b"),
    };

    for (int i = 0; i < files.length; ++i) {
      createRandomFile(new File(getDirectory(), files[i].getPath()));
    }

    final FileDistributionHandlerImplementation fileDistributionHandler =
      new FileDistributionHandlerImplementation(
        new CacheIdentity() {},
        getDirectory(),
        files,
        distributionControl,
        updateableAgentCacheState);

    distributionControlStubFactory.assertSuccess("clearFileCaches",
                                                 CacheHighWaterMark.class);

    final FileDistributionHandler.Result result0 =
      fileDistributionHandler.sendNextFile();

    assertEquals(50, result0.getProgressInCents());
    assertEquals("a", result0.getFileName());

    distributionControlStubFactory.assertSuccess("sendFile",
                                                 FileContents.class,
                                                 CacheHighWaterMark.class);

    final File earliestFile = new File(getDirectory(), files[0].getPath());

    updateableAgentCacheStateStubFactory.assertSuccess(
      "updateStarted", new Long(earliestFile.lastModified()));
    updateableAgentCacheStateStubFactory.assertNoMoreCalls();

    final FileDistributionHandler.Result result1 =
      fileDistributionHandler.sendNextFile();

    assertEquals(100, result1.getProgressInCents());
    assertEquals("b", result1.getFileName());

    distributionControlStubFactory.assertSuccess("sendFile",
                                                 FileContents.class,
                                                 CacheHighWaterMark.class);

    updateableAgentCacheStateStubFactory.assertNoMoreCalls();

    final FileDistributionHandler.Result result2 =
      fileDistributionHandler.sendNextFile();

    assertNull(result2);

    distributionControlStubFactory.assertSuccess(
      "setHighWaterMark", CacheHighWaterMark.class);
    distributionControlStubFactory.assertNoMoreCalls();

    updateableAgentCacheStateStubFactory.assertSuccess("updateComplete");
    updateableAgentCacheStateStubFactory.assertNoMoreCalls();
  }
}
