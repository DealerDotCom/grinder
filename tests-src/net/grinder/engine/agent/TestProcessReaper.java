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

package net.grinder.engine.agent;

import junit.framework.TestCase;


/**
 *  Unit tests for <code>ProcessReaper</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestProcessReaper extends TestCase {

  public void testProcessReaper() throws Exception {

    final String[] commandArray = {
      "java",
      "-classpath",
      "build/tests-classes",
      EchoClass.class.getName(),
    };

    final Runtime runtime = Runtime.getRuntime();
    final Process process1 = runtime.exec(commandArray);
    final Process process2 = runtime.exec(commandArray);
    
    final ProcessReaper reaper = ProcessReaper.getInstance();

    reaper.add(process1);

    assertProcessIsRunning(process1);
    assertProcessIsRunning(process2);

    reaper.run();

    assertProcessTerminated(process1);
    assertProcessIsRunning(process2);

    reaper.add(process2);
    reaper.remove(process2);

    reaper.run();

    assertProcessIsRunning(process2);

    reaper.add(process2);

    reaper.run();

    assertProcessTerminated(process2);
  }

  private void assertProcessIsRunning(Process process) {
    try {
      process.exitValue();
      fail("Process not running");
    }
    catch (IllegalThreadStateException e) {
    }
  }

  private void assertProcessTerminated(Process process)
    throws InterruptedException {

    // Won't return if process is running. Actual exit value is
    // platform specific, and sometimes 0 on win32!
    final int exitValue = process.waitFor();
  }
}
