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

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

import net.grinder.console.communication.DistributionControl;
import net.grinder.console.distribution.CacheHighWaterMarkImplementation.CacheIdentity;
import net.grinder.console.distribution.FileChangeWatcher.FileChangedListener;
import net.grinder.console.distribution.FileDistributionImplementation.CacheIdentityImplementation;
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

    final Directory directory1 = new Directory(getDirectory());

    final FileDistributionImplementation fileDistribution =
      new FileDistributionImplementation(distributionControl,
                                         directory1,
                                         m_matchIgnoredPattern);

    distributionControlStubFactory.assertNoMoreCalls();

    final File anotherFile = new File(getDirectory(), "foo");
    anotherFile.mkdir();
    final Directory directory2 = new Directory(anotherFile);

    final FileDistributionHandler fileDistributionHandler1 =
      fileDistribution.getHandler();

    distributionControlStubFactory.assertSuccess("clearFileCaches");
    distributionControlStubFactory.assertNoMoreCalls();

    // Convince the AgentCacheState that its up to date.
    AgentCacheStateImplementation agentCacheState =
      (AgentCacheStateImplementation)fileDistribution.getAgentCacheState();
    agentCacheState.updateStarted(anotherFile.lastModified());
    agentCacheState.updateComplete();

    // Test with same directory.
    final FileDistributionHandler fileDistributionHandler2 =
      fileDistribution.getHandler();

    assertNotSame(fileDistributionHandler1, fileDistributionHandler2);
    distributionControlStubFactory.assertNoMoreCalls();

    // Test with a different directory, should now need to flush the
    // file caches.
    fileDistribution.setDirectory(directory2);

    final FileDistributionHandler fileDistributionHandler3 =
      fileDistribution.getHandler();

    assertNotSame(fileDistributionHandler1, fileDistributionHandler3);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler3);
    distributionControlStubFactory.assertSuccess("clearFileCaches");

    // Again, convince the AgentCacheState that its up to date.
    agentCacheState.updateStarted(anotherFile.lastModified());
    agentCacheState.updateComplete();

    // Test with the same directory, but a different pattern, should
    // need to flush.
    fileDistribution.setFileFilterPattern(m_matchAllPattern);

    final FileDistributionHandler fileDistributionHandler4 =
      fileDistribution.getHandler();

    assertNotSame(fileDistributionHandler1, fileDistributionHandler4);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler4);
    assertNotSame(fileDistributionHandler3, fileDistributionHandler4);
    distributionControlStubFactory.assertSuccess("clearFileCaches");
    distributionControlStubFactory.assertNoMoreCalls();

    // Mark cache as up to date.
    agentCacheState.updateStarted(anotherFile.lastModified());
    agentCacheState.updateComplete();

    // Test with original directory.
    fileDistribution.setDirectory(directory1);

    final FileDistributionHandler fileDistributionHandler5 =
      fileDistribution.getHandler();

    assertNotSame(fileDistributionHandler1, fileDistributionHandler5);
    assertNotSame(fileDistributionHandler2, fileDistributionHandler5);
    assertNotSame(fileDistributionHandler3, fileDistributionHandler5);
    assertNotSame(fileDistributionHandler4, fileDistributionHandler5);
    distributionControlStubFactory.assertSuccess("clearFileCaches");

    agentCacheState.setOutOfDate();

    final FileDistributionHandler fileDistributionHandler6 =
      fileDistribution.getHandler();

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

    final FileDistributionImplementation fileDistribution =
      new FileDistributionImplementation(
        distributionControl,
        agentCacheState,
        directory,
        m_matchIgnoredPattern);
    fileDistribution.addFileChangedListener(filesChangedListener);

    fileDistribution.scanDistributionFiles();
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

    fileDistribution.setDirectory(directory);
    fileDistribution.setFileFilterPattern(m_matchAllPattern);
    fileDistribution.scanDistributionFiles();
    assertEquals(0, agentCacheState.getEarliestFileTime());
    fileListenerStubFactory.assertNoMoreCalls();

    file1.delete();
    file1.createNewFile();
    file2.delete();
    file2.createNewFile();
    file2.setLastModified(file1.lastModified() + 5000);

    fileDistribution.setFileFilterPattern(m_matchIgnoredPattern);
    fileDistribution.scanDistributionFiles();
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
    fileDistribution.scanDistributionFiles();
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

    fileDistribution.setDirectory(new Directory(testDirectory));
    fileDistribution.scanDistributionFiles();
    // Directories no longer affect cache.
    assertEquals(Long.MAX_VALUE,
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
    fileDistribution.setDirectory(directory);
    fileDistribution.scanDistributionFiles();
    assertEquals(0, agentCacheStateStubFactory.getEarliestOutOfDateTime());
    fileListenerStubFactory.resetCallHistory();

    // Test with r/o directory, just for coverage's sake.
    final Directory subdirectory =
      new Directory(new File(getDirectory(), "subdirectory"));
    subdirectory.create();
    final File f1 = new File(subdirectory.getFile(), "file");
    f1.createNewFile();

    FileUtilities.setCanAccess(subdirectory.getFile(), false);

    fileDistribution.setDirectory(subdirectory);
    fileDistribution.scanDistributionFiles();

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

  public void testFilter() throws Exception {
    final Pattern pattern = Pattern.compile("^a.*[^/]$|.*exclude.*|.*b/$");

    final FileFilter filter =
      new FileDistributionImplementation.FixedPatternFileFilter(10000L,
                                                                pattern);

    final String[] acceptableFilenames = new String[] {
      "DoesntStartWithA.acceptable",
      "blah blah blah",
      "blah-file-store",
    };

    for (int i = 0; i < acceptableFilenames.length; ++i) {
      final File f = new File(getDirectory(), acceptableFilenames[i]);
      f.createNewFile();
      assertTrue(f.getPath() + " is acceptable", filter.accept(f));
    }

    final String[] unacceptableFileNames = new String[] {
      "exclude me",
      "a file beginning with a",
      "a directory ending with b",
    };

    for (int i = 0; i < unacceptableFileNames.length; ++i) {
      final File f = new File(getDirectory(), unacceptableFileNames[i]);
      f.createNewFile();

      assertTrue(f.getPath() + " is unacceptable", !filter.accept(f));
    }

    final File timeFile = new File(getDirectory(), "time file");
    timeFile.createNewFile();
    assertTrue(timeFile.getPath() + " is acceptable", filter.accept(timeFile));
    timeFile.setLastModified(123L);
    assertTrue(timeFile.getPath() + " is unacceptable",
               !filter.accept(timeFile));

    // Add an error margin, as Linux does not support setting the modification
    // date with millisecond precision.
    timeFile.setLastModified(101001L);
    assertTrue(timeFile.getPath() + " is acceptable", filter.accept(timeFile));

    final String[] acceptableDirectoryNames = new String[] {
      "a directory ending with b.not",
      "include me",
    };

    for (int i = 0; i < acceptableDirectoryNames.length; ++i) {
      final File f = new File(getDirectory(), acceptableDirectoryNames[i]);
      f.mkdir();
      assertTrue(f.getPath() + " is acceptable", filter.accept(f));
    }

    final String[] unacceptableDirectoryNames = new String[] {
      "a directory ending with b",
      "exclude me",
    };

    for (int i = 0; i < unacceptableDirectoryNames.length; ++i) {
      final File f = new File(getDirectory(), unacceptableDirectoryNames[i]);
      f.mkdir();
      assertTrue(f.getPath() + " is unacceptable", !filter.accept(f));
    }

    final File timeDirectory = new File(getDirectory(), "time directory");
    timeDirectory.mkdir();
    assertTrue(timeDirectory.getPath() + " is acceptable",
               filter.accept(timeDirectory));
    timeDirectory.setLastModified(123L);
    assertTrue(timeDirectory.getPath() + " is acceptable",
               filter.accept(timeDirectory));

    final File fileStoreDirectory = new File(getDirectory(), "foo-file-store");
    fileStoreDirectory.mkdir();
    assertTrue(fileStoreDirectory.getPath() + " is acceptable",
               filter.accept(fileStoreDirectory));

    final File readMeFile = new File(fileStoreDirectory, "README.txt");
    readMeFile.createNewFile();
    assertTrue(fileStoreDirectory.getPath() + " is unacceptable",
               !filter.accept(fileStoreDirectory));

    readMeFile.delete();
    assertTrue(fileStoreDirectory.getPath() + " is acceptable",
               filter.accept(fileStoreDirectory));
  }

  public void testIsDistributableFile() throws Exception {
    final Pattern pattern = Pattern.compile("^a.*[^/]$|.*exclude.*|.*b/$");

    final FileDistributionImplementation fileDistribution =
      new FileDistributionImplementation(null, null, pattern);
    final FileFilter filter = fileDistribution.getDistributionFileFilter();

    final String[] acceptableFilenames = new String[] {
      "DoesntStartWithA.acceptable",
      "blah blah blah",
      "blah-file-store",
    };

    for (int i = 0; i < acceptableFilenames.length; ++i) {
      final File f = new File(getDirectory(), acceptableFilenames[i]);
      f.createNewFile();
      assertTrue(f.getPath() + " is distributable", filter.accept(f));
    }

    final String[] unacceptableFileNames = new String[] {
      "exclude me",
      "a file beginning with a",
      "a directory ending with b",
    };

    for (int i = 0; i < unacceptableFileNames.length; ++i) {
      final File f = new File(getDirectory(), unacceptableFileNames[i]);
      f.createNewFile();

      assertTrue(f.getPath() + " is not distributable", !filter.accept(f));
    }

    // filter should still be valid if pattern changes.
    fileDistribution.setFileFilterPattern(Pattern.compile(".*exclude.*"));

    assertTrue(!filter.accept(new File(getDirectory(), "exclude me")));
    assertTrue(
      filter.accept(new File(getDirectory(), "a file begining with a")));

  }

  public void testCacheIdentityImplementation() throws Exception {
    final Directory d1 = new Directory();
    final Directory d2 = new Directory(new File("blah"));

    final CacheIdentity i0 =
      new CacheIdentityImplementation(d1, m_matchAllPattern);
    final CacheIdentity i1 =
      new CacheIdentityImplementation(d1, m_matchAllPattern);
    final CacheIdentity i2 =
      new CacheIdentityImplementation(d2, m_matchAllPattern);
    final CacheIdentity i3 =
      new CacheIdentityImplementation(d2, m_matchIgnoredPattern);

    assertEquals(i0, i0);
    assertEquals(i0, i1);
    assertEquals(i1, i0);
    assertEquals(i0.hashCode(), i1.hashCode());
    assertTrue(!i1.equals(i2));
    assertTrue(!i2.equals(i1));
    assertTrue(!i2.equals(i3));
    assertTrue(!i3.equals(i2));
    assertEquals(i3, i3);
    assertFalse(i2.equals(null));
    assertFalse(i1.equals(this));
  }
}
