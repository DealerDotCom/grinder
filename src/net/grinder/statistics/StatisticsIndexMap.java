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

import net.grinder.common.GrinderException;


/**
 * A register of statistic index objects. Each statistic has a unique index
 * object and a name. The index objects are used with {@link
 * net.grinder.script.Statistics} and the names can be used in expressions
 * (see {@link ExpressionView}). Statistics can either be <em>long</em> integer
 * values (see {@link #getIndexForLong}) or <em>double</em> floating-point
 * values ({@link #getIndexForDouble}).
 *
 * <p>
 * There is a special type of index object for <em>sample</em> statistics, see
 * {@link LongSampleIndex},{@link #getIndexForLongSample},
 * {@link DoubleSampleIndex},{@link #getIndexForDoubleSample}. Sample
 * statistics are the result of a series of sample values. The values can be
 * either <code>long</code> s or <code>double</code>s. Sample statistics
 * have three attribute values that can be read: the count (number of samples),
 * sum (total of all sample values), and sample variance. Each of these
 * attributes has its own index object, e.g. see
 * {@link LongSampleIndex#getVarianceIndex}.
 * </p>
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
 * The standard sample statistics used by The Grinder are:
 * <ul>
 * <li><code>timedTests</code></li>
 * </ul>
 * </p>
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

  private StatisticsIndexMap() {
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

    m_longSampleMap.put("timedTests", new LongSampleIndex(nextLongIndex++,
                                                          nextLongIndex++,
                                                          nextDoubleIndex++));

    m_numberOfDoubles = nextDoubleIndex;
    m_numberOfLongs = nextLongIndex;
  }

  boolean isDoubleIndex(String statisticKey) {
    return m_doubleMap.get(statisticKey) != null;
  }

  boolean isLongIndex(String statisticKey) {
    return m_longMap.get(statisticKey) != null;
  }

  boolean isDoubleSampleIndex(String statisticKey) {
    return m_doubleSampleMap.get(statisticKey) != null;
  }

  boolean isLongSampleIndex(String statisticKey) {
    return m_longSampleMap.get(statisticKey) != null;
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
   * @return The index object.
   * @exception GrinderException If <code>statisticName</code> is not
   * registered.
   */
  public DoubleIndex getIndexForDouble(String statisticName)
    throws GrinderException {
    final Object existing = m_doubleMap.get(statisticName);

    if (existing == null) {
      throw new GrinderException("Unknown key '" + statisticName + "'");
    }

    return (DoubleIndex)existing;
  }

  /**
   * Obtain the index object for the named long statistic.
   *
   * @param statisticName The statistic name.
   * @return The index object.
   * @exception GrinderException If <code>statisticName</code> is not
   * registered.
   */
  public LongIndex getIndexForLong(String statisticName)
    throws GrinderException {
    final Object existing = m_longMap.get(statisticName);

    if (existing == null) {
      throw new GrinderException("Unknown key '" + statisticName + "'");
    }

    return (LongIndex)existing;
  }

  /**
   * Obtain the index object for the named double sample tatistic.
   *
   * @param statisticName The statistic name.
   * @return The index object.
   * @exception GrinderException If <code>statisticName</code> is not
   * registered.
   */
  public DoubleSampleIndex getIndexForDoubleSample(String statisticName)
    throws GrinderException {
    final Object existing = m_doubleSampleMap.get(statisticName);

    if (existing == null) {
      throw new GrinderException("Unknown key '" + statisticName + "'");
    }

    return (DoubleSampleIndex)existing;
  }

  /**
   * Obtain the index object for the named long statistic.
   *
   * @param statisticName The statistic name.
   * @return The index object.
   * @exception GrinderException If <code>statisticName</code> is not
   * registered.
   */
  public LongSampleIndex getIndexForLongSample(String statisticName)
    throws GrinderException {
    final Object existing = m_longSampleMap.get(statisticName);

    if (existing == null) {
      throw new GrinderException("Unknown key '" + statisticName + "'");
    }

    return (LongSampleIndex)existing;
  }

  /**
   * Marker interface for a statistic indices.
   */
  interface Index extends Serializable {
  }

  /**
   * Base for index classes.
   */
  abstract static class AbstractSimpleIndex implements Index {

    private final int m_value;
    private final boolean m_readOnly;

    protected AbstractSimpleIndex(int i, boolean readOnly) {
      m_value = i;
      m_readOnly = readOnly;
    }

    protected AbstractSimpleIndex(int i) {
      this(i, false);
    }

    public final int getValue() {
      return m_value;
    }

    public final boolean isReadOnly() {
      return m_readOnly;
    }
  }

  /**
   * Opaque object that represents a double statistic.
   */
  public static final class DoubleIndex extends AbstractSimpleIndex {
    private DoubleIndex(int i) {
      super(i);
    }

    private DoubleIndex(int i, boolean readOnly) {
      super(i, readOnly);
    }
  }

  /**
   * Opaque object that represents a long statistic.
   */
  public static final class LongIndex extends AbstractSimpleIndex {
    private LongIndex(int i) {
      super(i);
    }

    private LongIndex(int i, boolean readOnly) {
      super(i, readOnly);
    }
  }

  /**
   * Marker interface for a sample statistic indices.
   */
  public interface SampleIndex extends Index {
    /**
     * @return Returns the index for the Count statistic.
     */
    LongIndex getCountIndex();

    /**
     * @return Returns the index for the Variance statistic.
     */
    DoubleIndex getVarianceIndex();
  }

  /**
   * Abstract implementation of {@link SampleIndex}.
   */
  static class AbstractSampleIndex implements SampleIndex {
    private final LongIndex m_countIndex;
    private final DoubleIndex m_varianceIndex;

    AbstractSampleIndex(int countIndexValue, int varianceIndexValue) {
      m_countIndex = new LongIndex(countIndexValue, true);
      m_varianceIndex = new DoubleIndex(varianceIndexValue, true);
    }

    /**
     * @return Returns the index for the Count statistic.
     */
    public final LongIndex getCountIndex() {
      return m_countIndex;
    }

    /**
     * @return Returns the index for the Variance statistic.
     */
    public final DoubleIndex getVarianceIndex() {
      return m_varianceIndex;
    }
  }

  /**
   * Object that represents a sample statistic with <code>double</code> sample
   * values.
   */
  public static final class DoubleSampleIndex extends AbstractSampleIndex {
    private final DoubleIndex m_sumIndex;

    private DoubleSampleIndex(int sumIndexValue,
                              int countIndexValue,
                              int varianceIndexValue) {
      super(countIndexValue, varianceIndexValue);
      m_sumIndex = new DoubleIndex(sumIndexValue, true);
    }

    /**
     * Get the index object for our sum.
     *
     * @return The index object.
     */
    public DoubleIndex getSumIndex() {
      return m_sumIndex;
    }
  }

  /**
   * Object that represents a sample statistic with <code>long</code> sample
   * values.
   */
  public static final class LongSampleIndex extends AbstractSampleIndex {
    private final LongIndex m_sumIndex;

    private LongSampleIndex(int sumIndexValue,
                            int countIndexValue,
                            int varianceIndexValue) {
      super(countIndexValue, varianceIndexValue);
      m_sumIndex = new LongIndex(sumIndexValue, true);
    }

    /**
     * Get the index object for our sum.
     *
     * @return The index object.
     */
    public LongIndex getSumIndex() {
      return m_sumIndex;
    }
  }
}
