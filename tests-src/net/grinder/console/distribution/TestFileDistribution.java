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
    final Set connectedAgents = new HashSet();
    final DistributionControlStubFactory distributionControlStubFactory =
      new DistributionControlStubFactory(connectedAgents);
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

    distributionControlStubFactory.assertSuccess("getConnectedAgents");
    distributionControlStubFactory.assertSuccess("clearFileCaches");
    distributionControlStubFactory.assertNoMoreCalls();

    // Test with same directory.
    final FileDistributionHandler fileDistributionHandler2 =
      fileDistribution.getHandler(directory1, matchNonePattern);

    assertNotSame(fileDistributionHandler1, fileDistributionHandler2);
    distributionControlStubFactory.assertSuccess("getConnectedAgents");
    distributionControlStubFactory.assertNoMoreCalls();

    // Test with a different directory.
    final FileDistributionHandler fileDistributionHandler3 =
      fileDistribution.getHandler(directory2, matchNonePattern);

    assertNotSame(fileDistributionHandler1, fileDistributionHandler3);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler3);
    distributionControlStubFactory.assertSuccess("getConnectedAgents");
    distributionControlStubFactory.assertSuccess("clearFileCaches");

    // Test with the same directory, but a different pattern.
    final FileDistributionHandler fileDistributionHandler4 =
      fileDistribution.getHandler(directory2, matchAllPattern);

    assertNotSame(fileDistributionHandler1, fileDistributionHandler4);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler4);
    assertNotSame(fileDistributionHandler3, fileDistributionHandler4);
    distributionControlStubFactory.assertSuccess("getConnectedAgents");
    distributionControlStubFactory.assertSuccess("clearFileCaches");
    distributionControlStubFactory.assertNoMoreCalls();

    // Test with original directory.
    final FileDistributionHandler fileDistributionHandler5 =
      fileDistribution.getHandler(directory1, matchAllPattern);

    assertNotSame(fileDistributionHandler1, fileDistributionHandler5);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler5);
    assertNotSame(fileDistributionHandler3, fileDistributionHandler5);
    assertNotSame(fileDistributionHandler4, fileDistributionHandler5);
    distributionControlStubFactory.assertSuccess("getConnectedAgents");
    distributionControlStubFactory.assertSuccess("clearFileCaches");

    // Test with new connected agents.
    connectedAgents.add(new Integer(1));
    connectedAgents.add(new Integer(2));

    final FileDistributionHandler fileDistributionHandler6 =
      fileDistribution.getHandler(directory1, matchAllPattern);

    assertNotSame(fileDistributionHandler5, fileDistributionHandler6);
    distributionControlStubFactory.assertSuccess("getConnectedAgents");
    distributionControlStubFactory.assertSuccess("clearFileCaches");

    connectedAgents.remove(new Integer(1));

    final FileDistributionHandler fileDistributionHandler7 =
      fileDistribution.getHandler(directory1, matchAllPattern);

    assertNotSame(fileDistributionHandler6, fileDistributionHandler7);
    distributionControlStubFactory.assertSuccess("getConnectedAgents");
    distributionControlStubFactory.assertNoMoreCalls();
  }

  public void testAgentCacheStateImplementation() throws Exception {
    final FileDistribution.AgentCacheStateImplementation
      cacheState = new FileDistribution.AgentCacheStateImplementation();

    assertEquals(-1, cacheState.getEarliestFileTime());
    assertTrue(cacheState.getOutOfDate());

    cacheState.updateStarted();
    assertEquals(-1, cacheState.getEarliestFileTime());
    assertTrue(cacheState.getOutOfDate());

    cacheState.updateComplete();
    assertFalse(cacheState.getOutOfDate());
    assertFalse(cacheState.getEarliestFileTime() == -1);

    cacheState.setOutOfDate();
    assertTrue(cacheState.getOutOfDate());
    assertFalse(cacheState.getEarliestFileTime() == -1);
  }

  public static class DistributionControlStubFactory
    extends RandomStubFactory {

    private final Set m_connectedAgents;

    public DistributionControlStubFactory(Set connectedAgents) {
      super(DistributionControl.class);
      m_connectedAgents = connectedAgents;
    }

    public DistributionControl getDistributionControl() {
      return (DistributionControl)getStub();
    }

    public Set override_getConnectedAgents(Object proxy) {
      return new HashSet(m_connectedAgents);
    }
  }
}
