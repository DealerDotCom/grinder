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

package net.grinder.console.distribution;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;

import net.grinder.console.communication.DistributionControl;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.Directory;


/**
 * Unit test for {@link FileDistribution}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestFileDistribution extends AbstractFileTestCase {

  public void testGetHandler() throws Exception {
    final DistributionControlStubFactory distributionControlStubFactory =
      new DistributionControlStubFactory();
    final DistributionControl distributionControl =
      distributionControlStubFactory.getDistributionControl();

    final FileDistribution fileDistribution =
      new FileDistribution(distributionControl);

    distributionControlStubFactory.assertNoMoreCalls();

    final Directory directory1 = new Directory(getDirectory());

    final File anotherFile = new File(getDirectory(), "foo");
    anotherFile.mkdir();
    final Directory directory2 = new Directory(anotherFile);

    final Perl5Compiler compiler = new Perl5Compiler();
    final Pattern matchNonePattern = compiler.compile("^$");
    final Pattern matchAllPattern = compiler.compile(".*");

    final FileDistributionHandler fileDistributionHandler1 =
      fileDistribution.getHandler(directory1, matchNonePattern);

    distributionControlStubFactory.assertSuccess("clearFileCaches");
    distributionControlStubFactory.assertNoMoreCalls();

    // Convince the AgentCacheState that its up to date.
    AgentCacheStateImplementation agentCacheState =
      (AgentCacheStateImplementation)fileDistribution.getAgentCacheState();
    agentCacheState.updateStarted();
    agentCacheState.updateComplete();

    // Test with same directory.
    final FileDistributionHandler fileDistributionHandler2 =
      fileDistribution.getHandler(directory1, matchNonePattern);

    assertNotSame(fileDistributionHandler1, fileDistributionHandler2);
    distributionControlStubFactory.assertNoMoreCalls();

    // Test with a different directory, should now need to flush the
    // file caches.
    final FileDistributionHandler fileDistributionHandler3 =
      fileDistribution.getHandler(directory2, matchNonePattern);

    assertNotSame(fileDistributionHandler1, fileDistributionHandler3);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler3);
    distributionControlStubFactory.assertSuccess("clearFileCaches");

    // Again, convince the AgentCacheState that its up to date.
    agentCacheState.updateStarted();
    agentCacheState.updateComplete();

    // Test with the same directory, but a different pattern, should
    // need to flush.
    final FileDistributionHandler fileDistributionHandler4 =
      fileDistribution.getHandler(directory2, matchAllPattern);

    assertNotSame(fileDistributionHandler1, fileDistributionHandler4);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler4);
    assertNotSame(fileDistributionHandler3, fileDistributionHandler4);
    distributionControlStubFactory.assertSuccess("clearFileCaches");
    distributionControlStubFactory.assertNoMoreCalls();

    // Mark cache as up to date.
    agentCacheState.updateStarted();
    agentCacheState.updateComplete();

    // Test with original directory.
    final FileDistributionHandler fileDistributionHandler5 =
      fileDistribution.getHandler(directory1, matchAllPattern);

    assertNotSame(fileDistributionHandler1, fileDistributionHandler5);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler5);
    assertNotSame(fileDistributionHandler3, fileDistributionHandler5);
    assertNotSame(fileDistributionHandler4, fileDistributionHandler5);
    distributionControlStubFactory.assertSuccess("clearFileCaches");

    agentCacheState.setOutOfDate();

    final FileDistributionHandler fileDistributionHandler6 =
      fileDistribution.getHandler(directory1, matchAllPattern);

    assertNotSame(fileDistributionHandler5, fileDistributionHandler6);
    distributionControlStubFactory.assertSuccess("clearFileCaches");

    distributionControlStubFactory.assertNoMoreCalls();
  }

  public static class DistributionControlStubFactory
    extends RandomStubFactory {

    public DistributionControlStubFactory() {
      super(DistributionControl.class);
    }

    public DistributionControl getDistributionControl() {
      return (DistributionControl)getStub();
    }
  }
}
