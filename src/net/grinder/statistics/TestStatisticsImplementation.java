// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

package net.grinder.statistics;

import java.io.IOException;
import java.io.ObjectInput;

import net.grinder.common.GrinderException;
import net.grinder.util.Serialiser;


/**
 * Wire a {@link RawStatisticsImplementation} to implement {@link
 * TestStatistics}.
 *
 * TODO probably want to get rid of this, or at least change it to use
 * delegation.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class TestStatisticsImplementation
  extends RawStatisticsImplementation implements TestStatistics {

  private static final StatisticsIndexMap.LongIndex s_errorsIndex;
  private static final StatisticsIndexMap.LongIndex s_untimedTestsIndex;
  private static final StatisticsIndexMap.LongSampleIndex s_timedTestsIndex;

  static {
    final StatisticsIndexMap indexMap = StatisticsIndexMap.getInstance();

    try {
      s_errorsIndex = indexMap.getIndexForLong("errors");
      s_untimedTestsIndex = indexMap.getIndexForLong("untimedTests");
      s_timedTestsIndex = indexMap.getIndexForLongSample("timedTests");
    }
    catch (GrinderException e) {
      throw new ExceptionInInitializerError(
        "Assertion failure, " +
        "TestStatisticsImplementation could not initialise: " +
        e.getMessage());
    }
  }

  /**
   * Constructor.
   */
  public TestStatisticsImplementation() {
  }

  /**
   * Copy constructor.
   *
   * @param other Object to copy. Caller is responsible for synchronisation.
   */
  protected TestStatisticsImplementation(TestStatisticsImplementation other) {
    super(other);
  }

  /**
   * Clone this object. We need to override so that the cloned object is of
   * the correct type. See notes in {@link RawStatisticsImplementation#snapshot}
   * for why we don't use {@link Object#clone}.
   *
   * @return A copy of this TestStatisticsImplementation.
   */
  public synchronized RawStatistics snapshot() {
    return new TestStatisticsImplementation(this);
  }

  /**
   * Efficient externalisation method used by {@link
   * TestStatisticsFactory#writeStatisticsExternal}.
   *
   * @param in Handle to the output stream.
   * @param serialiser <code>Serialiser</code> helper object.
   * @exception IOException If an error occurs.
   */
  public TestStatisticsImplementation(ObjectInput in, Serialiser serialiser)
    throws IOException {
    super(in, serialiser);
  }

  public long getTests() {
    return
      getCount(s_timedTestsIndex) +
      getValue(s_untimedTestsIndex);
  }

  public long getErrors() {
    return getValue(s_errorsIndex);
  }

  public double getAverageTestTime() {
    final long count = getCount(s_timedTestsIndex);

    return
      count == 0 ?
      Double.NaN : getSum(s_timedTestsIndex) / (double)count;
  }
}
