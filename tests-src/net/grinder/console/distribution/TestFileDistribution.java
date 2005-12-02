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
import java.util.regex.Pattern;

import net.grinder.console.communication.DistributionControl;
import net.grinder.console.distribution.FileChangeWatcher.FileChangedListener;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.Directory;


/**
 * Unit test for {@link FileDistributionImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestFileDistribution extends AbstractFileTestCase {

  public void testGetHandler() throws Exception {
    final RandomStubFactory distributionControlStubFactory =
      new RandomStubFactory(DistributionControl.class);
    final DistributionControl distributionControl =
      (DistributionControl)distributionControlStubFactory.getStub();

    final FileDistribution fileDistribution =
      new FileDistributionImplementation(distributionControl);

    distributionControlStubFactory.assertNoMoreCalls();

    final Directory directory1 = new Directory(getDirectory());

    final File anotherFile = new File(getDirectory(), "foo");
    anotherFile.mkdir();
    final Directory directory2 = new Directory(anotherFile);

    final Pattern matchNonePattern = Pattern.compile("^$");
    final Pattern matchAllPattern = Pattern.compile(".*");

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

  public void testScanDistributionFiles() throws Exception {
    final RandomStubFactory distributionControlStubFactory =
      new RandomStubFactory(DistributionControl.class);
    final DistributionControl distributionControl =
      (DistributionControl)distributionControlStubFactory.getStub();

    final UpdateableAgentCacheStateStubFactory
      agentCacheStateStubFactory =
        new UpdateableAgentCacheStateStubFactory();

    final UpdateableAgentCacheState agentCacheState =
      agentCacheStateStubFactory.getUpdateableAgentCacheState();

    final RandomStubFactory fileListenerStubFactory =
      new RandomStubFactory(FileChangedListener.class);
    final FileChangedListener filesChangedListener =
      (FileChangedListener)fileListenerStubFactory.getStub();

    final Directory directory = new Directory(getDirectory());

    final FileDistribution fileDistribution =
      new FileDistributionImplementation(distributionControl, agentCacheState);
    fileDistribution.addFileChangedListener(filesChangedListener);

    fileDistribution.scanDistributionFiles(directory);
    assertEquals(0, agentCacheState.getEarliestFileTime());

    final File file1 = new File(getDirectory(), "file1");
    file1.createNewFile();
    final File file2 = new File(getDirectory(), "file2");
    file2.createNewFile();
    final File oldFile = new File(getDirectory(), "file3");
    oldFile.createNewFile();
    oldFile.setLastModified(0);
    file2.setLastModified(file1.lastModified() + 5000);

    fileDistribution.scanDistributionFiles(directory);
    assertEquals(file1.lastModified(),
                 agentCacheStateStubFactory.getEarliestOutOfDateTime());

    final CallData filesChangedCall = fileListenerStubFactory.getCallData();
    assertEquals("filesChanged", filesChangedCall.getMethodName());
    assertEquals(1, filesChangedCall.getParameters().length);
    final File[] changedFiles = (File[])(filesChangedCall.getParameters()[0]);
    assertEquals(2, changedFiles.length);
    assertTrue(changedFiles[0].equals(file1) && changedFiles[1].equals(file2) ||
               changedFiles[0].equals(file2) && changedFiles[1].equals(file1));

    fileListenerStubFactory.assertNoMoreCalls();

    agentCacheStateStubFactory.setEarliestFileTime(file2.lastModified() - 10);
    agentCacheStateStubFactory.resetOutOfDate();
    fileDistribution.scanDistributionFiles(directory);
    assertEquals(file2.lastModified(),
                 agentCacheStateStubFactory.getEarliestOutOfDateTime());

    // Even if the cache has older out of date times, we only scan from the
    // last scan time.
    agentCacheStateStubFactory.setEarliestFileTime(0);
    agentCacheStateStubFactory.resetOutOfDate();
    fileDistribution.scanDistributionFiles(directory);
    assertEquals(file1.lastModified(),
                 agentCacheStateStubFactory.getEarliestOutOfDateTime());
    fileListenerStubFactory.resetCallHistory();

    // Do some checks with directories
    agentCacheStateStubFactory.resetOutOfDate();
    final File testDirectory = new File(getDirectory(), "test");
    testDirectory.mkdir();
    final File directory1 = new File(getDirectory(), "test/dir1");
    directory1.mkdir();
    final File oldDirectory = new File(getDirectory(), "test/dir3");
    oldDirectory.mkdir();
    final File directory2 = new File(getDirectory(), "test/dir3/dir2");
    directory2.mkdir();
    oldDirectory.setLastModified(0);
    file2.setLastModified(file1.lastModified() + 5000);

    fileDistribution.scanDistributionFiles(new Directory(testDirectory));
    assertEquals(directory1.lastModified(),
                 agentCacheStateStubFactory.getEarliestOutOfDateTime());

    final CallData directoriesChangedCall =
      fileListenerStubFactory.getCallData();
    assertEquals("filesChanged", directoriesChangedCall.getMethodName());
    assertEquals(1, directoriesChangedCall.getParameters().length);
    final File[] changedDirectories =
      (File[])(directoriesChangedCall.getParameters()[0]);
    assertEquals(2, changedDirectories.length);
    assertTrue(changedDirectories[0].equals(directory1) &&
               changedDirectories[1].equals(directory2) ||
               changedDirectories[0].equals(directory2) &&
               changedDirectories[1].equals(directory1));

    fileListenerStubFactory.assertNoMoreCalls();
  }

  public static class UpdateableAgentCacheStateStubFactory
    extends RandomStubFactory {

    private long m_earliestFileTime;
    private long m_earliestOutOfDateTime = Long.MAX_VALUE;

    public UpdateableAgentCacheStateStubFactory() {
      super(UpdateableAgentCacheState.class);
    }

    public UpdateableAgentCacheState getUpdateableAgentCacheState() {
      return (UpdateableAgentCacheState)getStub();
    }

    public long override_getEarliestFileTime(Object proxy) {
      return m_earliestFileTime;
    }

    public void setEarliestFileTime(long t) {
      m_earliestFileTime = t;
    }

    public long getEarliestOutOfDateTime() {
      return m_earliestOutOfDateTime;
    }

    public void override_setOutOfDate(Object proxy, long t) {
      if (t < m_earliestOutOfDateTime) {
        m_earliestOutOfDateTime = t;
      }
    }

    public void resetOutOfDate() {
      m_earliestOutOfDateTime = Long.MAX_VALUE;
    }
  }
}
