// Copyright (C) 2005, 2006, 2007 Philip Aston
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

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.regex.Pattern;

import net.grinder.console.communication.DistributionControl;
import net.grinder.console.distribution.FileChangeWatcher.FileChangedListener;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.FileUtilities;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.Directory;


/**
 * Unit test for {@link FileDistributionImplementation}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestFileDistribution extends AbstractFileTestCase {

  private final Pattern m_matchIgnoredPattern =
    Pattern.compile("^.grinder/$");
  private final Pattern m_matchAllPattern = Pattern.compile(".*");

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

    final FileDistributionHandler fileDistributionHandler1 =
      fileDistribution.getHandler(directory1, m_matchIgnoredPattern);

    distributionControlStubFactory.assertSuccess("clearFileCaches");
    distributionControlStubFactory.assertNoMoreCalls();

    // Convince the AgentCacheState that its up to date.
    AgentCacheStateImplementation agentCacheState =
      (AgentCacheStateImplementation)fileDistribution.getAgentCacheState();
    agentCacheState.updateStarted(anotherFile.lastModified());
    agentCacheState.updateComplete();

    // Test with same directory.
    final FileDistributionHandler fileDistributionHandler2 =
      fileDistribution.getHandler(directory1, m_matchIgnoredPattern);

    assertNotSame(fileDistributionHandler1, fileDistributionHandler2);
    distributionControlStubFactory.assertNoMoreCalls();

    // Test with a different directory, should now need to flush the
    // file caches.
    final FileDistributionHandler fileDistributionHandler3 =
      fileDistribution.getHandler(directory2, m_matchIgnoredPattern);

    assertNotSame(fileDistributionHandler1, fileDistributionHandler3);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler3);
    distributionControlStubFactory.assertSuccess("clearFileCaches");

    // Again, convince the AgentCacheState that its up to date.
    agentCacheState.updateStarted(anotherFile.lastModified());
    agentCacheState.updateComplete();

    // Test with the same directory, but a different pattern, should
    // need to flush.
    final FileDistributionHandler fileDistributionHandler4 =
      fileDistribution.getHandler(directory2, m_matchAllPattern);

    assertNotSame(fileDistributionHandler1, fileDistributionHandler4);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler4);
    assertNotSame(fileDistributionHandler3, fileDistributionHandler4);
    distributionControlStubFactory.assertSuccess("clearFileCaches");
    distributionControlStubFactory.assertNoMoreCalls();

    // Mark cache as up to date.
    agentCacheState.updateStarted(anotherFile.lastModified());
    agentCacheState.updateComplete();

    // Test with original directory.
    final FileDistributionHandler fileDistributionHandler5 =
      fileDistribution.getHandler(directory1, m_matchAllPattern);

    assertNotSame(fileDistributionHandler1, fileDistributionHandler5);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler5);
    assertNotSame(fileDistributionHandler3, fileDistributionHandler5);
    assertNotSame(fileDistributionHandler4, fileDistributionHandler5);
    distributionControlStubFactory.assertSuccess("clearFileCaches");

    agentCacheState.setOutOfDate();

    final FileDistributionHandler fileDistributionHandler6 =
      fileDistribution.getHandler(directory1, m_matchAllPattern);

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

    fileDistribution.scanDistributionFiles(directory, m_matchIgnoredPattern);
    assertEquals(0, agentCacheState.getEarliestFileTime());

    final CallData filesChangedCall =
      fileListenerStubFactory.assertSuccess("filesChanged", File[].class);
    final File[] changedFiles = (File[])(filesChangedCall.getParameters()[0]);
    assertEquals(1, changedFiles.length);
    assertTrue(changedFiles[0].equals(directory.getFile()));

    fileListenerStubFactory.assertNoMoreCalls();

    final File file1 = new File(getDirectory(), "file1");
    file1.createNewFile();
    final File file2 = new File(getDirectory(), "file2");
    file2.createNewFile();
    final File oldFile = new File(getDirectory(), "file3");
    oldFile.createNewFile();
    oldFile.setLastModified(0);
    file2.setLastModified(file1.lastModified() + 5000);

    fileDistribution.scanDistributionFiles(directory, m_matchAllPattern);
    assertEquals(0, agentCacheState.getEarliestFileTime());
    fileListenerStubFactory.assertNoMoreCalls();

    file1.delete();
    file1.createNewFile();
    file2.delete();
    file2.createNewFile();
    file2.setLastModified(file1.lastModified() + 5000);

    fileDistribution.scanDistributionFiles(directory, m_matchIgnoredPattern);
    assertEquals(file1.lastModified(),
                 agentCacheStateStubFactory.getEarliestOutOfDateTime());

    final CallData filesChangedCall2 =
      fileListenerStubFactory.assertSuccess("filesChanged", File[].class);
    final File[] changedFiles2 = (File[])(filesChangedCall2.getParameters()[0]);
    assertEquals(3, changedFiles2.length);
    AssertUtilities.assertArrayContainsAll(
      changedFiles2,
      new File[] { directory.getFile(), file1, file2 } );

    fileListenerStubFactory.assertNoMoreCalls();

    // Even if the cache has older out of date times, we only scan from the
    // last scan time.
    final File file4 = new File(getDirectory(), "file4");
    file4.createNewFile();
    agentCacheStateStubFactory.setEarliestFileTime(0);
    agentCacheStateStubFactory.resetOutOfDate();
    fileDistribution.scanDistributionFiles(directory, m_matchIgnoredPattern);
    assertEquals(file4.lastModified(),
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

    fileDistribution.scanDistributionFiles(
      new Directory(testDirectory), m_matchIgnoredPattern);
    assertEquals(directory1.lastModified(),
                 agentCacheStateStubFactory.getEarliestOutOfDateTime());

    final CallData directoriesChangedCall =
      fileListenerStubFactory.assertSuccess("filesChanged", File[].class);
    final File[] changedDirectories =
      (File[])(directoriesChangedCall.getParameters()[0]);
    assertEquals(3, changedDirectories.length);
    AssertUtilities.assertArrayContainsAll(changedDirectories,
      new File[] { testDirectory, directory1, directory2 } );

    fileListenerStubFactory.assertNoMoreCalls();

    // If the cache has been reset, we scan the lot.
    agentCacheStateStubFactory.setEarliestFileTime(-1);
    agentCacheStateStubFactory.firePropertyChangeListener();
    fileDistribution.scanDistributionFiles(directory, m_matchIgnoredPattern);
    assertEquals(0, agentCacheStateStubFactory.getEarliestOutOfDateTime());
    fileListenerStubFactory.resetCallHistory();

    // Test with r/o directory, just for coverage's sake.
    final Directory subdirectory =
      new Directory(new File(getDirectory(), "subdirectory"));
    subdirectory.create();
    final File f1 = new File(subdirectory.getFile(), "file");
    f1.createNewFile();

    FileUtilities.setCanAccess(subdirectory.getFile(), false);

    fileDistribution.scanDistributionFiles(subdirectory, m_matchIgnoredPattern);

    assertEquals(f1.lastModified(),
                 agentCacheStateStubFactory.getEarliestOutOfDateTime());

    FileUtilities.setCanAccess(subdirectory.getFile(), true);
  }

  public static class UpdateableAgentCacheStateStubFactory
    extends RandomStubFactory {

    private long m_earliestFileTime;
    private long m_earliestOutOfDateTime = Long.MAX_VALUE;
    private PropertyChangeListener m_listener;

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

    public void override_addListener(
      Object proxy, PropertyChangeListener listener) {
      m_listener = listener;
    }

    public void firePropertyChangeListener() {
      m_listener.propertyChange(null);
    }
  }
}
