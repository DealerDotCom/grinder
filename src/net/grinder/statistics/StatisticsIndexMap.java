// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
// Copyright (C) 2004 John Stanford White
// Copyright (C) 2004 Calum Fitzgerald
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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * A register of statistic index objects.
 *
 * <p>Each statistic has a unique index object and a name. The index objects are
 * used with {@link net.grinder.script.Statistics} and the names can be used in
 * expressions (see {@link ExpressionView}). Statistics can either be
 * <em>long</em> integer values (see {@link #getIndexForLong}) or
 * <em>double</em> floating-point values ({@link #getIndexForDouble}).
 *
 * <p>
 * The standard long statistics used by The Grinder are:
 * <ul>
 * <li><code>errors</code></li>
 * <li><code>untimedTests</code></li>
 * <li><code>period</code></li>
 * </ul>
 * </p>
 *
 * <p>
 * Additionally, there are five long statistics for use by scripts and custom
 * plugins, <code>userLong0</code>,<code>userLong1</code>, ...
 * <code>userLong4</code>.
 * </p>
 *
 * <p>
 * The standard double statistics used by The Grinder are:
 * <ul>
 * <li><code>peakTPS</code></li>
 * </ul>
 * </p>
 *
 * <p>
 * Additionally, there are five double statistics for use by scripts and custom
 * plugins, <code>userDouble0</code>,<code>userDouble1</code>, ...
 * <code>userDouble4</code>.
 * </p>
 *
 * <p>
 * There is a special type of index object for <em>sample</em> statistics, see
 * {@link LongSampleIndex},{@link #getIndexForLongSample},
 * {@link DoubleSampleIndex},{@link #getIndexForDoubleSample}. Sample
 * statistics are the result of a series of sample values. The values can be
 * either <code>long</code>s or <code>double</code>s. Sample statistics
 * have three attribute values that can be read: the <em>count</em> (number of
 * samples), <em>sum</em> (total of all sample values), and sample
 * <em>variance</em>. These attributes can be queried using
 * the appropriate expression function (e.g. <em>count()</em>), see
 * {@link ExpressionView}.
 * </p>
 *
 * <p>
 * The standard long sample statistics used by The Grinder are:
 * <ul>
 * <li><code>timedTests</code></li>
 * </ul>
 * </p>
 *
 * <p>There are no standard double sample statistics.</p>
 *
 * <p>There is currently no provision for user specific sample statistics.</p>
 *
 * @author Philip Aston
 * @version $Revision$
 */
public final class StatisticsIndexMap implements Serializable {
  private static final StatisticsIndexMap s_processInstance =
    new StatisticsIndexMap();

  private final Map m_doubleMap = new HashMap();
  private final Map m_longMap = new HashMap();
  private final Map m_doubleSampleMap = new HashMap();
  private final Map m_longSampleMap = new HashMap();

  // These are bigger than m_doubleMap.size() and m_longMap.size()
  // as the sample indicies also use slots.
  private final int m_numberOfDoubles;
  private final int m_numberOfLongs;

  /**
   * Special slot for the HTTP plugin so it doesn't steal "user"
   * indicies.
   */
  public static final String HTTP_PLUGIN_RESPONSE_STATUS_KEY =
    "httpplugin.responseStatusKey";

  /**
   * Special slot for the HTTP plugin so it doesn't steal "user"
   * indicies.
   */
  public static final String HTTP_PLUGIN_RESPONSE_LENGTH_KEY =
    "httpplugin.responseLengthKey";

  /**
   * Special slot for the HTTP plugin so it doesn't steal "user"
   * indices.
   */
  public static final String HTTP_PLUGIN_RESPONSE_ERRORS_KEY =
    "httpplugin.responseErrorsKey";

  /**
   * Special slot for the HTTP plugin so it doesn't steal "user"
   * indices.
   */
  public static final String HTTP_PLUGIN_DNS_TIME_KEY =
    "httpplugin.dnsTimeKey";

  /**
   * Special slot for the HTTP plugin so it doesn't steal "user"
   * indices.
   */
  public static final String HTTP_PLUGIN_CONNECT_TIME_KEY =
    "httpplugin.connectTimeKey";

  /**
   * Special slot for the HTTP plugin so it doesn't steal "user"
   * indices.
   */
  public static final String HTTP_PLUGIN_FIRST_BYTE_TIME_KEY =
    "httpplugin.firstByteTimeKey";

  /**
   * Singleton accessor.
   *
   * @return The singleton <code>StatisticsIndexMap</code>.
   */
  public static StatisticsIndexMap getInstance() {
    return s_processInstance;
  }

  /**
   * Constructor.
   *
   * <p>Package scope for unit tests.</p>
   */
  StatisticsIndexMap() {
    // Set up standard statistic index values. When adding new values
    // or changing the order, you should also change the serialVersionUID
    // of TestStatisticsMap.
    int nextLongIndex = 0;
    int nextDoubleIndex = 0;

    m_longMap.put("errors", new LongIndex(nextLongIndex++));
    m_longMap.put("untimedTests", new LongIndex(nextLongIndex++));
    m_longMap.put("period", new LongIndex(nextLongIndex++));
    m_longMap.put(HTTP_PLUGIN_RESPONSE_STATUS_KEY,
                  new LongIndex(nextLongIndex++));
    m_longMap.put(HTTP_PLUGIN_RESPONSE_LENGTH_KEY,
                  new LongIndex(nextLongIndex++));
    m_longMap.put(HTTP_PLUGIN_RESPONSE_ERRORS_KEY,
                  new LongIndex(nextLongIndex++));
    m_longMap.put(HTTP_PLUGIN_DNS_TIME_KEY,
                  new LongIndex(nextLongIndex++));
    m_longMap.put(HTTP_PLUGIN_CONNECT_TIME_KEY,
                  new LongIndex(nextLongIndex++));
    m_longMap.put(HTTP_PLUGIN_FIRST_BYTE_TIME_KEY,
                  new LongIndex(nextLongIndex++));
    m_longMap.put("userLong0", new LongIndex(nextLongIndex++));
    m_longMap.put("userLong1", new LongIndex(nextLongIndex++));
    m_longMap.put("userLong2", new LongIndex(nextLongIndex++));
    m_longMap.put("userLong3", new LongIndex(nextLongIndex++));
    m_longMap.put("userLong4", new LongIndex(nextLongIndex++));

    m_doubleMap.put("peakTPS", new DoubleIndex(nextDoubleIndex++));
    m_doubleMap.put("userDouble0", new DoubleIndex(nextDoubleIndex++));
    m_doubleMap.put("userDouble1", new DoubleIndex(nextDoubleIndex++));
    m_doubleMap.put("userDouble2", new DoubleIndex(nextDoubleIndex++));
    m_doubleMap.put("userDouble3", new DoubleIndex(nextDoubleIndex++));
    m_doubleMap.put("userDouble4", new DoubleIndex(nextDoubleIndex++));

    createLongSampleIndex("timedTests",
                          new LongIndex(nextLongIndex++),
                          new LongIndex(nextLongIndex++),
                          new DoubleIndex(nextDoubleIndex++));

    m_numberOfDoubles = nextDoubleIndex;
    m_numberOfLongs = nextLongIndex;
  }

  int getNumberOfDoubles() {
    return m_numberOfDoubles;
  }

  int getNumberOfLongs() {
    return m_numberOfLongs;
  }

  Collection getDoubleSampleIndicies() {
    return m_doubleSampleMap.values();
  }

  Collection getLongSampleIndicies() {
    return m_longSampleMap.values();
  }

  /**
   * Obtain the index object for the named double statistic.
   *
   * @param statisticName The statistic name.
   * @return The index object, or <code>null</code> if there is no such
   * double statistic.
   */
  public DoubleIndex getDoubleIndex(String statisticName) {
    return (DoubleIndex)m_doubleMap.get(statisticName);
  }

  /**
   * Obtain the index object for the named long statistic.
   *
   * @param statisticName The statistic name.
   * @return The index object, or <code>null</code> if there is no such
   * long statistic.
   */
  public LongIndex getLongIndex(String statisticName) {
    return (LongIndex)m_longMap.get(statisticName);
  }

  /**
   * Obtain the index object for the named double sample statistic.
   *
   * @param statisticName The statistic name.
   * @return The index object, or <code>null</code> if there is no such
   * double sample statistic.
   */
  public DoubleSampleIndex getDoubleSampleIndex(String statisticName) {
    return (DoubleSampleIndex)m_doubleSampleMap.get(statisticName);
  }

  /**
   * Obtain the index object for the named long statistic.
   *
   * @param statisticName The statistic name.
   * @return The index object, or <code>null</code> if there is no such
   * long sample statistic.
   */
  public LongSampleIndex getLongSampleIndex(String statisticName) {
    return (LongSampleIndex)m_longSampleMap.get(statisticName);
  }

  /**
   * Factory for {@link LongSampleIndex}s.
   *
   * <p>If this is made public and called from somewhere other than our
   * constructor, we need to address synchronisation.</p>
   *
   * @param statisticName Name to register index under.
   * @param sumIndex Index to hold sum.
   * @param countIndex Index to hold count.
   * @param varianceIndex Index to hold variance.
   * @return The new index.
   */
  private LongSampleIndex createLongSampleIndex(String statisticName,
                                                LongIndex sumIndex,
                                                LongIndex countIndex,
                                                DoubleIndex varianceIndex) {
    final LongSampleIndex result =
      new LongSampleIndex(sumIndex, countIndex, varianceIndex);

    m_longSampleMap.put(statisticName, result);

    return result;
  }

  /**
   * Factory for {@link DoubleSampleIndex}s.
   *
   * <p>Package scope for unit tests.</p>
   *
   * <p>If this is made public and called from somewhere other than our
   * constructor, we need to address synchronisation.</p>
   *
   * @param statisticName Name to register index under.
   * @param sumIndex Index to hold sum.
   * @param countIndex Index to hold count.
   * @param varianceIndex Index to hold variance.
   * @return The new index.
   */
  DoubleSampleIndex createDoubleSampleIndex(String statisticName,
                                            DoubleIndex sumIndex,
                                            LongIndex countIndex,
                                            DoubleIndex varianceIndex) {
    final DoubleSampleIndex result =
      new DoubleSampleIndex(sumIndex, countIndex, varianceIndex);

    m_doubleSampleMap.put(statisticName, result);

    return result;
  }

  /**
   * For unit tests to remove {@link DoubleSampleIndex}s they've created.
   *
   * @param statisticName Name of the DoubleSampleIndex.
   */
  void removeDoubleSampleIndex(String statisticName) {
    m_doubleSampleMap.remove(statisticName);
  }

  /**
   * Base for index classes.
   */
  abstract static class AbstractSimpleIndex {

    private final int m_value;

    protected AbstractSimpleIndex(int i) {
      m_value = i;
    }

    public final int getValue() {
      return m_value;
    }
  }

  /**
   * Opaque object that represents a double statistic.
   */
  public static final class DoubleIndex extends AbstractSimpleIndex {
    private DoubleIndex(int i) {
      super(i);
    }
  }

  /**
   * Opaque object that represents a long statistic.
   */
  public static final class LongIndex extends AbstractSimpleIndex {
    private LongIndex(int i) {
      super(i);
    }
  }

  /**
   * Base class for sample statistic indices.
   */
  static class SampleIndex {
    private final LongIndex m_countIndex;
    private final DoubleIndex m_varianceIndex;

    protected SampleIndex(LongIndex countIndex, DoubleIndex varianceIndex) {
      m_countIndex = countIndex;
      m_varianceIndex = varianceIndex;
    }

    /**
     * Get the index object for our count (the number of samples).
     *
     * <p>Package scope to prevent direct write access. External clients should
     * use the {@link RawStatistics} or {@link StatisticExpression} interfaces.
     * </p>
     *
     * @return The index object.
     */
    final LongIndex getCountIndex() {
      return m_countIndex;
    }

    /**
     * Get the index object for our variance.
     *
     * <p>Package scope to prevent direct write access. External clients should
     * use the {@link RawStatistics} or {@link StatisticExpression} interfaces.
     * </p>
     *
     * @return The index object.
     */
    final DoubleIndex getVarianceIndex() {
      return m_varianceIndex;
    }
  }

  /**
   * Object that represents a sample statistic with <code>double</code> sample
   * values.
   */
  public static final class DoubleSampleIndex extends SampleIndex {
    private final DoubleIndex m_sumIndex;

    private DoubleSampleIndex(DoubleIndex sumIndex,
                              LongIndex countIndex,
                              DoubleIndex varianceIndex) {
      super(countIndex, varianceIndex);
      m_sumIndex = sumIndex;
    }

    /**
     * Get the index object for our sum.
     *
     * <p>Package scope to prevent direct write access. External clients should
     * use the {@link RawStatistics} or {@link StatisticExpression} interfaces.
     * </p>
     *
     * @return The index object.
     */
    DoubleIndex getSumIndex() {
      return m_sumIndex;
    }
  }

  /**
   * Object that represents a sample statistic with <code>long</code> sample
   * values.
   */
  public static final class LongSampleIndex extends SampleIndex {
    private final LongIndex m_sumIndex;

    private LongSampleIndex(LongIndex sumIndex,
                            LongIndex countIndex,
                            DoubleIndex varianceIndex) {
      super(countIndex, varianceIndex);
      m_sumIndex = sumIndex;
    }

    /**
     * Get the index object for our sum.
     *
     * <p>Package scope to prevent direct write access. External clients should
     * use the {@link RawStatistics} or {@link StatisticExpression} interfaces.
     * </p>
     *
     * @return The index object.
     */
    LongIndex getSumIndex() {
      return m_sumIndex;
    }
  }
}
