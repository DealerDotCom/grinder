// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

package net.grinder.engine.agent;

import java.io.BufferedReader;
import java.io.PrintWriter;


/**
 * This class is used to redirect the standard output and error to
 * disk files. It reads characters from a BufferedRead and prints them
 * out in a PrintStream.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
class Redirector implements java.lang.Runnable {
  private final PrintWriter m_printWriter;
  private final BufferedReader m_bufferedReader;

  /**
   * The constructor. It starts a thread that executes 
   * the <tt>run</tt> method.
   * 
   */      
  public Redirector(PrintWriter printWriter, BufferedReader bufferedReader) {
    m_printWriter = printWriter;
    m_bufferedReader = bufferedReader;

    final Thread t = new Thread(this, m_printWriter.toString());
    t.start(); 
  }
    
  /**
   * This method reads characters from a BufferedReader and prints
   * them out in a PrintWriter.
   */    
  public void run() {
    try{
      String s;

      while ((s = m_bufferedReader.readLine()) != null) {
	m_printWriter.println(s);
      }

      m_bufferedReader.close();
    }
    catch(Exception e){
      System.err.println(e);
    }
  }
}
