// Copyright (C) 2001, 2002, 2003, 2004 Philip Aston
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


/**
 * Abstract base class which times a method and returns whether it
 * exectuted within the given range.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public abstract class Time {
  private final double m_expectedMin;
  private final double m_expectedMax;

  public Time(double expectedMin, double expectedMax) {
    m_expectedMin = expectedMin;
    m_expectedMax = expectedMax; // A bit of leeway.
  }
  
  public abstract void doIt() throws Exception;

  public boolean run() throws Exception {
    final long then = System.currentTimeMillis();
    doIt();
    final long time = System.currentTimeMillis() - then;
    
    return m_expectedMin <= time && m_expectedMax >= time;
  }
}
