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

package net.grinder.testutility;

import junit.framework.TestCase;

import java.io.File;


/**
 * Abstract test case that managaes a temporary directory.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public abstract class AbstractFileTestCase extends TestCase {

  private File m_directory;

  public void setUp() throws Exception {
    m_directory = File.createTempFile(getClass().getName(), "test");
    m_directory.delete();
    m_directory.mkdir();
    m_directory.deleteOnExit();
  }

  public void tearDown() throws Exception {
    delete(m_directory);
  }

  private static void delete(File f) throws Exception {

    if (f.isDirectory()) {
      final File[] children = f.listFiles();

      for (int i=0; i<children.length; ++i) {
        delete(children[i]);
      }
    }

    if (!f.delete()) {
      System.err.println("Could not delete file '" + f + "'");
    }
  }

  protected final File getDirectory() {
    return m_directory;
  }
}