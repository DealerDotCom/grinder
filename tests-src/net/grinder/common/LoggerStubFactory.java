// Copyright (C) 2000, 2001, 2002, 2003, 2004 Philip Aston
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

import java.io.PrintWriter;

import net.grinder.testutility.CountingPrintWriter;
import net.grinder.testutility.RandomStubFactory;


public class LoggerStubFactory extends RandomStubFactory {

  private CountingPrintWriter m_errorLineCounter = new CountingPrintWriter();
  private CountingPrintWriter m_outputLineCounter = new CountingPrintWriter();

  public LoggerStubFactory() {
    super(Logger.class);
  }

  protected LoggerStubFactory(Class c) {
    super(c);
  }

  public Logger getLogger() {
    return (Logger) getStub();
  }

  public PrintWriter override_getErrorLogWriter(Object proxy) {
    return m_errorLineCounter;
  }

  public PrintWriter override_getOutputLogWriter(Object proxy) {
    return m_outputLineCounter;
  }

  public int getNumberOfErrorLines() {
    return m_errorLineCounter.getCount();
  }

  public int getNumberOfOutputLines() {
    return m_outputLineCounter.getCount();
  }
}

