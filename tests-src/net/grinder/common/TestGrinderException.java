// Copyright (C) 2002, 2003, 2004 Philip Aston
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

package net.grinder.common;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * Unit test for {@link GrinderException}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestGrinderException extends TestCase {

  public void testRemoveCommonSuffix() throws Exception {
    final String oldLineSeparator = System.getProperty("line.separator");

    try {

      final String[] lineSeparators = {
        "\n",
        "\n\r",
        "\r",
      };
      
      for (int i = 0; i <lineSeparators.length; ++i) {
        final String lineSeparator = lineSeparators[i];
        System.setProperty("line.separator", lineSeparator);

        StringBuffer b1 = new StringBuffer(
          "Hello there" + lineSeparator + "world");

        StringBuffer b2 = new StringBuffer(
          "Goodbye there" + lineSeparator + "world");

        assertTrue(GrinderException.removeCommonSuffix(b1, b2));
        assertEquals("Hello there", b1.toString());

        b1 = new StringBuffer("Hello world");
        b2 = new StringBuffer("Goodbye world  ");

        assertFalse(GrinderException.removeCommonSuffix(b1, b2));
        assertEquals("Hello world", b1.toString());

        b1 = new StringBuffer("Hello world");
        b2 = new StringBuffer("");

        assertFalse(GrinderException.removeCommonSuffix(b1, b2));
        assertEquals("Hello world", b1.toString());

        b1 = new StringBuffer("");
        b2 = new StringBuffer("Goodbye");

        assertFalse(GrinderException.removeCommonSuffix(b1, b2));
        assertEquals("", b1.toString());

        b1 = new StringBuffer("foo" + lineSeparator + "bah");
        b2 = new StringBuffer("Goodbye");

        assertFalse(GrinderException.removeCommonSuffix(b1, b2));
        assertEquals("foo" + lineSeparator + "bah", b1.toString());

        b1 = new StringBuffer("foo" + lineSeparator + "  ");
        b2 = new StringBuffer("Goodbye");

        assertFalse("White space terminator so should return false",
                    GrinderException.removeCommonSuffix(b1, b2));
        assertEquals("foo", b1.toString());

        b1 = new StringBuffer(
          "Several more" + lineSeparator + lineSeparator + "lines   of" +
          lineSeparator + "fun to ponder");

        b2 = new StringBuffer(
          "Many" + lineSeparator + lineSeparator + "more" + lineSeparator +
          "lines of" + lineSeparator + "fun to ponder  ");

        assertTrue(GrinderException.removeCommonSuffix(b1, b2));
        assertEquals("Several more", b1.toString());
      }
    }
    finally {
        System.setProperty("line.separator", oldLineSeparator);
    }
  }

  public void testPrintStackTrace() throws Exception {
    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);
	
    final GrinderException e1 = createDeeperException();
    final GrinderException e2 = new GrinderException("Exception 2", e1);

    e2.printStackTrace(printWriter);
    final String s = stringWriter.toString();

    assertEquals(1, countOccurrences("createException", s));
    assertEquals(1, countOccurrences("createDeeperException", s));
    assertEquals(2, countOccurrences("testPrintStackTrace", s));
    assertEquals(1, countOccurrences("Method.invoke", s));
  }

  public void testPrintStackTraceWithNestedNonGrinderException()
    throws Exception {

    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);
	
    final Exception e1 = new RuntimeException();
    final GrinderException e2 = new GrinderException("Exception 2", e1);

    e2.printStackTrace(printWriter);
    final String s = stringWriter.toString();

    assertEquals(1, countOccurrences("RuntimeException", s));
    assertEquals(2, countOccurrences(
                   "testPrintStackTraceWithNestedNonGrinderException", s));
    assertEquals(1, countOccurrences("Method.invoke", s));
  }

  private class WeirdException extends RuntimeException {
    public void printStackTrace(PrintWriter w) {
      w.println("Unconventional stack trace");
      w.flush();
    }
  }

  public void testPrintStackTraceWithNestedUnconventionalException()
    throws Exception {

    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);
	
    final Exception e1 = new WeirdException();
    final GrinderException e2 = new GrinderException("Exception 2", e1);

    e2.printStackTrace(printWriter);
    final String s = stringWriter.toString();

    assertEquals(1, countOccurrences(
                   "testPrintStackTraceWithNestedUnconventionalException", s));
    assertEquals(1, countOccurrences("Method.invoke", s));
    assertEquals(0, countOccurrences("truncated", s));
  }

  public void testPrintStackTraceWithPrintStream() throws Exception {
    final ByteArrayOutputStream byteArrayOutputStream =
      new ByteArrayOutputStream();
    final PrintStream printStream = new PrintStream(byteArrayOutputStream);
	
    final GrinderException e1 = createDeeperException();
    final GrinderException e2 = new GrinderException("Exception 2", e1);

    e2.printStackTrace(printStream);
    final String s = new String(byteArrayOutputStream.toByteArray());

    assertEquals(1, countOccurrences("createException", s));
    assertEquals(1, countOccurrences("createDeeperException", s));
    assertEquals(2, countOccurrences("testPrintStackTrace", s));
    assertEquals(1, countOccurrences("Method.invoke", s));
  }

  public void testPrintStackTraceWithDefaultStream() throws Exception {

    final ByteArrayOutputStream byteArrayOutputStream =
      new ByteArrayOutputStream();
    final PrintStream printStream = new PrintStream(byteArrayOutputStream);

    final GrinderException e1 = createDeeperException();
    final GrinderException e2 = new GrinderException("Exception 2", e1);

    final PrintStream oldStderr = System.err;
    System.setErr(printStream);

    try {
      e2.printStackTrace();
    }
    finally {
      System.setErr(oldStderr);
    }

    final String s = new String(byteArrayOutputStream.toByteArray());

    assertEquals(1, countOccurrences("createException", s));
    assertEquals(1, countOccurrences("createDeeperException", s));
    assertEquals(2, countOccurrences("testPrintStackTrace", s));
    assertEquals(1, countOccurrences("Method.invoke", s));
  }

  private GrinderException createException() {
    return new GrinderException("an exception");
  }

  private GrinderException createDeeperException() {
    return createException();
  }

  private int countOccurrences(String pattern, String original) {
    int result = 0;
    int p = -1;

    while ((p=original.indexOf(pattern, p + 1)) >= 0) {
      ++result;
    }

    return result;
  }
}
