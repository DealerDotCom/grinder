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

package net.grinder.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;


/**
 * Pairing of relative filename and file contents.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class FileContents implements Serializable {

  private static final long serialVersionUID = 8373961429207921091L;

  private final File m_filename;
  private final byte[] m_contents;

  /**
   * Constructor. Builds a FileContents from local filesystem.
   *
   * @param baseDirectory Base directory used to resolve relative filenames.
   * @param originalFile Relative filename.
   * @exception IOException if an error occurs
   */
  public FileContents(File baseDirectory, File originalFile)
    throws IOException {

    m_filename = originalFile;

    final InputStream inputStream =
      new FileInputStream(new File(baseDirectory, originalFile.getPath()));

    final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
    new CopyStreamRunnable(inputStream, byteOutputStream, true).run();

    m_contents = byteOutputStream.toByteArray();
  }


  /**
   * Return the relative filename.
   *
   * @return a <code>File</code> value
   */
  public File getFilename() {
    return m_filename;
  }

  /**
   * Return the file contents.
   *
   * @return a <code>byte[]</code> value
   */
  public byte[] getContents() {
    return m_contents;
  }
}
