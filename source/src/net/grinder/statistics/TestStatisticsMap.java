// Copyright (C) 2000 - 2008 Philip Aston
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

package net.grinder.statistics;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.TreeMap;

import net.grinder.common.AbstractTestSemantics;
import net.grinder.common.Test;


/**
 * A map of test numbers to {@link StatisticsSet}s.
 *
 * <p>Test statistics synchronisation occurs at the granularity of the
 * contained {@link StatisticsSet} instances. The map is synchronised
 * on the <code>TestStatisticsMap</code> itself.</p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class TestStatisticsMap implements java.io.Externalizable {

  // The serialVersionUID should be incremented whenever the default
  // statistic indices are changed in StatisticsIndexMap, or
  // when the StatisticsSet externalisation methods are changed.
  private static final long serialVersionUID = 3L;

  private final transient StatisticsSetFactory m_statisticsSetFactory;

  /**
   * Use a TreeMap so we store in test number order. Synchronise on
   * this TestStatisticsMap before accessing.
   */
  private final Map m_data = new TreeMap();

  /**
   * Creates a new <code>TestStatisticsMap</code> instance.
   *
   * @param statisticsSetFactory A factory used to build {@link StatisticsSet}s.
   */
  public TestStatisticsMap(StatisticsSetFactory statisticsSetFactory) {
    m_statisticsSetFactory = statisticsSetFactory;
  }

  /**
   * Externalizable classes need a public default constructor.
   */
  public TestStatisticsMap() {
    // No choice but to initialise the StatisticsSetFactory from a singleton.
    // I hate Externalizable.
    this(
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory());
  }

  /**
   * Put a new {test, statistics} pair in the map.
   *
   * @param test A test.
   * @param statistics The test's statistics.
   */
  public void put(Test test, StatisticsSet statistics) {
    if (!(statistics instanceof StatisticsSetImplementation)) {
      throw new AssertionError(
        "StatisticsSet implementation not supported");
    }

    synchronized (this) {
      m_data.put(test, statistics);
    }
  }

  /**
   * Return the number of entries in the
   * <code>TestStatisticsMap</code>.
   *
   * @return an <code>int</code> value
   */
  public int size() {
    synchronized (this) {
      return m_data.size();
    }
  }

  /**
   * Add the values in another <code>TestStatisticsMap</code> to this
   * <code>TestStatisticsMap</code>.
   *
   * @param other The other <code>TestStatisticsMap</code>.
   */
  public void add(TestStatisticsMap other) {
    // Use an Iterator rather than ForEach as its important to be clear
    // about this versus the other.
    synchronized (other) {
      final Iterator otherIterator = other.new Iterator();

      while (otherIterator.hasNext()) {
        final Pair othersPair = otherIterator.next();

        final StatisticsSet statistics;

        synchronized (this) {
          final StatisticsSet existingStatistics =
            (StatisticsSet)m_data.get(othersPair.getTest());

          if (existingStatistics == null) {
            statistics = m_statisticsSetFactory.create();
            put(othersPair.getTest(), statistics);
          }
          else {
            statistics = existingStatistics;
          }
        }

        statistics.add(othersPair.getStatistics());
      }
    }
  }

  /**
   * Reset all our statistics and return a snapshot.
   *
   * @return The snapshot. Only Tests with non-zero statistics are included.
   */
  public TestStatisticsMap reset() {
    final TestStatisticsMap result =
      new TestStatisticsMap(m_statisticsSetFactory);

    new ForEach() {
      public void next(Test test, StatisticsSet statistics) {
        final StatisticsSet snapshot;

        synchronized (statistics) {
          snapshot = statistics.snapshot();
          statistics.reset();
        }

        if (!snapshot.isZero()) {
          result.put(test, snapshot);
        }
      }
    }
    .iterate();

    return result;
  }

  /**
   * Add up all the non-composite statistics.
   *
   * @return The sum of all the non-composite statistics.
   */
  public StatisticsSet nonCompositeStatisticsTotals() {
    final StatisticsSet result = m_statisticsSetFactory.create();

    new ForEach() {
      public void next(Test test, StatisticsSet statistics) {
        if (!statistics.isComposite()) {
          result.add(statistics);
        }
      }
    }
    .iterate();

    return result;
  }

  /**
   * Add up all the composite statistics.
   *
   * @return The sum of all the composite statistics.
   */
  public StatisticsSet compositeStatisticsTotals() {
    final StatisticsSet result = m_statisticsSetFactory.create();

    new ForEach() {
      public void next(Test test, StatisticsSet statistics) {
        if (statistics.isComposite()) {
          result.add(statistics);
        }
      }
    }
    .iterate();

    return result;
  }

  /**
   * Implement value based equality. Used by unit tests, so we don't
   * bother with synchronisation.
   *
   * @param o <code>Object</code> to compare to.
   * @return <code>true</code> if and only if the two objects are equal.
   */
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (o == null || o.getClass() != TestStatisticsMap.class) {
      return false;
    }

    final TestStatisticsMap otherMap = (TestStatisticsMap)o;

    if (m_data.size() != otherMap.m_data.size()) {
      return false;
    }

    final Iterator iterator = new Iterator();
    final Iterator otherIterator = otherMap.new Iterator();

    while (iterator.hasNext()) {
      final Pair pair = iterator.next();
      final Pair otherPair = otherIterator.next();

      if (!pair.getTest().equals(otherPair.getTest()) ||
          !pair.getStatistics().equals(otherPair.getStatistics())) {
        return false;
      }
    }

    return true;
  }

  /**
   * Defer to <code>Object.hashCode()</code>.
   *
   * <p>We define <code>hashCode</code> to keep Checkstyle happy, but
   * we don't use it.
   *
   * @return The hash code.
   */
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * Return a <code>String</code> representation of this
   * <code>TestStatisticsMap</code>.
   *
   * @return The <code>String</code>
   */
  public String toString() {
    final StringBuffer result = new StringBuffer();

    result.append("TestStatisticsMap = {");

    new ForEach() {
      public void next(Test test, StatisticsSet statisticsSet) {
        result.append("(");
        result.append(test);
        result.append(", ");
        result.append(statisticsSet);
        result.append(")");
      }
    }
    .iterate();

    result.append("}");

    return result.toString();
  }

  /**
   * Efficient externalisation method.
   *
   * @param out Handle to the output stream.
   * @exception IOException If an I/O error occurs.
   */
  public void writeExternal(ObjectOutput out) throws IOException {

    synchronized (this) {
      out.writeInt(m_data.size());

      final Iterator iterator = new Iterator();

      while (iterator.hasNext()) {
        final Pair pair = iterator.next();

        out.writeInt(pair.getTest().getNumber());

        // Its a class invariant that our StatisticsSets are all
        // StatisticsSetImplementations.
        m_statisticsSetFactory.writeStatisticsExternal(
          out, (StatisticsSetImplementation)pair.getStatistics());
      }
    }
  }

  /**
   * Efficient externalisation method. No synchronisation, assume that
   * we're being read into a new instance.
   *
   * @param in Handle to the input stream.
   * @exception IOException If an I/O error occurs.
   */
  public void readExternal(ObjectInput in) throws IOException {

    final int n = in.readInt();

    m_data.clear();

    for (int i = 0; i < n; i++) {
      m_data.put(new LightweightTest(in.readInt()),
                 m_statisticsSetFactory.readStatisticsExternal(in));
    }
  }

  /**
   * Light weight test implementation that the console uses.
   */
  private static final class LightweightTest extends AbstractTestSemantics {
    private final int m_number;

    public LightweightTest(int number) {
      m_number = number;
    }

    public int getNumber() {
      return m_number;
    }

    public String getDescription() {
      return "";
    }
  }

  /**
   * A type safe iterator. Should synchronise on the
   * <code>TestStatisticsMap</code> around use.
   *
   * <p>
   * See the simpler {@link TestStatisticsMap.ForEach} for cases where you
   * don't need control over advancing the iterator or to propagate exceptions
   * from the code that handles each item.
   * </p>
   */
  public final class Iterator {
    private final java.util.Iterator m_iterator;

    /**
     * Creates a new <code>Iterator</code> instance.
     */
    public Iterator() {
      m_iterator = m_data.entrySet().iterator();
    }

    /**
     * Check whether we are at the end of the {@link
     * TestStatisticsMap}.
     *
     * @return <code>true</code> if there is a next {@link
     * TestStatisticsMap.Pair}.
     */
    public boolean hasNext() {
      return m_iterator.hasNext();
    }

    /**
     * Get the next {@link TestStatisticsMap.Pair} from the {@link
     * TestStatisticsMap}.
     *
     * @return The next {@link TestStatisticsMap.Pair}.
     * @throws java.util.NoSuchElementException If there is no next element.
     */
    public Pair next() {
      final Map.Entry entry = (Map.Entry)m_iterator.next();
      final Test test = (Test)entry.getKey();
      final StatisticsSet statistics = (StatisticsSet)entry.getValue();

      return new Pair(test, statistics);
    }
  }

  /**
   * A type safe pair of a {@link net.grinder.common.Test} and a
   * {@link StatisticsSet}.
   */
  public static final class Pair {
    private final Test m_test;
    private final StatisticsSet m_statistics;

    private Pair(Test test, StatisticsSet statistics) {
      m_test = test;
      m_statistics = statistics;
    }

    /**
     * Get the {@link net.grinder.common.Test}.
     *
     * @return  The {@link net.grinder.common.Test}.
     */
    public Test getTest() {
      return m_test;
    }

    /**
     * Get the {@link StatisticsSet}.
     *
     * @return The {@link StatisticsSet}.
     */
    public StatisticsSet getStatistics() {
      return m_statistics;
    }
  }

  /**
   * Convenience visitor-like wrapper around an Iterator.
   */
  public abstract class ForEach {
    /**
     * Runs the iteration.
     */
    public void iterate() {
      synchronized (TestStatisticsMap.this) {
        final Iterator iterator = new Iterator();

        while (iterator.hasNext()) {
          final Pair pair = iterator.next();
          next(pair.getTest(), pair.getStatistics());
        }
      }
    }

    /**
     * Receives a call for each item in the iteration.
     *
     * @param test The item's Test.
     * @param statistics The item's statistics.
     */
    protected abstract void next(Test test, StatisticsSet statistics);
  }
}
