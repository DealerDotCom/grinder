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
import java.io.ObjectOutput;
import java.util.Arrays;

import net.grinder.util.Serialiser;


/**
 * Store an array of raw statistics as unsigned long values. Clients
 * can access individual values using an index obtained from a {@link
 * StatisticsIndexMap}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
class RawStatisticsImplementation implements RawStatistics {

  private final long[] m_longData;
  private final double[] m_doubleData;

  /**
   * Creates a new <code>RawStatisticsImplementation</code> instance.
   */
  public RawStatisticsImplementation() {
    m_longData =
      new long[
        StatisticsIndexMap.getInstance().getNumberOfLongIndicies()];

    m_doubleData = new double[StatisticsIndexMap.getInstance().
                              getNumberOfDoubleIndicies()];
  }

  /**
   * Copy constructor.
   *
   * @param other Object to copy. Caller is responsible for synchronisation.
   */
  protected RawStatisticsImplementation(RawStatisticsImplementation other) {
    this();
    System.arraycopy(other.m_longData, 0, m_longData, 0, m_longData.length);
    System.arraycopy(other.m_doubleData, 0,
                     m_doubleData, 0, m_doubleData.length);
  }

  /**
   * Reset this RawStatistics to default values. Allows instance to
   * be reused.
   *
   * Assuming the caller owns this
   * <code>RawStatisticsImplementation</code> (or they shouldn't be
   * reseting it), we don't synchronise
   */
  public final void reset() {
    Arrays.fill(m_longData, 0);
    Arrays.fill(m_doubleData, 0);
  }

  /**
   * Clone this object.
   *
   * We don't use {@link Object#clone} as that's such a hog's arse of a
   * mechanism. It prevents us from using final variables, requres a lot of
   * casting, and that we catch CloneNotSupported exceptions - even if we know
   * they won't be thrown.
   *
   * @return A copy of this RawStatisticsImplementation.
   */
  public synchronized RawStatistics snapshot() {
    return new RawStatisticsImplementation(this);
  }

  /**
   * Return the value specified by <code>index</code>.
   *
   * <p>Unfortunately the Java language specification does not make
   * long access atomic, so we must synchronise.</p>
   *
   * @param index The process specific index.
   * @return The value.
   */
  public final synchronized long getValue(StatisticsIndexMap.LongIndex index) {
    return m_longData[index.getValue()];
  }

  /**
   * Return the value specified by <code>index</code>.
   *
   * <p>Unfortunately the Java language specification does not make
   * double access atomic, so we must synchronise.</p>
   *
   * @param index The process specific index.
   * @return The value.
   */
  public final synchronized
    double getValue(StatisticsIndexMap.DoubleIndex index) {
    return m_doubleData[index.getValue()];
  }

  /**
   * Set the value specified by <code>index</code>.
   *
   * <p>Unfortunately the Java language specification does not make
   * long access atomic, so we must synchronise.</p>
   *
   * @param index The process specific index.
   * @param value The value.
   */
  public final synchronized
    void setValue(StatisticsIndexMap.LongIndex index, long value) {
    m_longData[index.getValue()] = value;
  }

  /**
   * Set the value specified by <code>index</code>.
   *
   * <p>Unfortunately the Java language specification does not make
   * double access atomic, so we must synchronise.</p>
   *
   * @param index The process specific index.
   * @param value The value.
   */
  public final synchronized
    void setValue(StatisticsIndexMap.DoubleIndex index, double value) {
    m_doubleData[index.getValue()] = value;
  }

  /**
   * Add <code>value</code> to the value specified by
   * <code>index</code>.
   *
   * <p>Synchronised to ensure we don't lose information.</p>.
   *
   * @param index The process specific index.
   * @param value The value.
   */
  public final synchronized void addValue(StatisticsIndexMap.LongIndex index,
                                          long value) {
    m_longData[index.getValue()] += value;
  }

  /**
   * Add <code>value</code> to the value specified by
   * <code>index</code>.
   *
   * <p>Synchronised to ensure we don't lose information.</p>.
   *
   * @param index The process specific index.
   * @param value The value.
   */
  public final synchronized void
    addValue(StatisticsIndexMap.DoubleIndex index, double value) {
    m_doubleData[index.getValue()] += value;
  }

  /**
   * Equivalent to <code>addValue(index, 1)</code>.
   *
   * @param index The process specific index.
   *
   * @see #addValue
   */
  public final void incrementValue(StatisticsIndexMap.LongIndex index) {
    addValue(index, 1);
  }

  /**
   * Add the values of another <code>RawStatistics</code> to ours.
   * Assumes we don't need to synchronise access to operand.
   *
   * <p><strong>Currently the implementation assumes that the
   * argument is actually a
   * <code>RawStatisticsImplementation</code></strong>.</p>
   *
   * <p>Synchronised to ensure we don't lose information.</p>.
   *
   * @param operand The <code>RawStatistics</code> value to add.
   */
  public final synchronized void add(RawStatistics operand) {
    final RawStatisticsImplementation operandImplementation =
      (RawStatisticsImplementation)operand;

    final long[] longData = operandImplementation.m_longData;

    for (int i = 0; i < longData.length; i++) {
      m_longData[i] += longData[i];
    }

    final double[] doubleData = operandImplementation.m_doubleData;

    for (int i = 0; i < doubleData.length; i++) {
      m_doubleData[i] += doubleData[i];
    }
  }

  /**
   * Implement value based equality. Mainly used by unit tests.
   *
   * @param o <code>Object</code> to compare to.
   * @return <code>true</code> if and only if the two objects are equal.
   */
  public final boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof RawStatisticsImplementation)) {
      return false;
    }

    final RawStatisticsImplementation otherStatistics =
      (RawStatisticsImplementation)o;

    final long[] otherLongData = otherStatistics.m_longData;

    for (int i = 0; i < m_longData.length; i++) {
      if (m_longData[i] != otherLongData[i]) {
        return false;
      }
    }

    for (int i = 0; i < m_doubleData.length; i++) {
      if (m_doubleData[i] != otherStatistics.m_doubleData[i]) {
        return false;
      }
    }

    return true;
  }

  /**
   * Defer to <code>Object.hashCode().</code>
   *
   * <p>We define <code>hashCode</code> to keep Checkstyle happy, but
   * we don't use it.
   *
   * @return The hash code.
   */
  public final int hashCode() {
    return super.hashCode();
  }

  /**
   * Return a <code>String</code> representation of this
   * <code>RawStatistics</code>.
   *
   * @return The <code>String</code>
   */
  public final String toString() {
    final StringBuffer result = new StringBuffer();

    result.append("RawStatistics = {");

    for (int i = 0; i < m_longData.length; i++) {
      result.append(m_longData[i]);

      if (i != m_longData.length - 1 || m_doubleData.length > 0) {
        result.append(", ");
      }
    }

    for (int i = 0; i < m_doubleData.length; i++) {
      result.append(m_doubleData[i]);

      if (i != m_doubleData.length - 1) {
        result.append(", ");
      }
    }

    result.append("}");

    return result.toString();
  }

  /**
   * Efficient externalisation method used by {@link
   * TestStatisticsFactory#writeStatisticsExternal}.
   *
   * <p>Synchronised to ensure a consistent view.</p>.
   *
   * @param out Handle to the output stream.
   * @param serialiser <code>Serialiser</code> helper object.
   * @exception IOException If an error occurs.
   */
  final synchronized void myWriteExternal(ObjectOutput out,
                                          Serialiser serialiser)
    throws IOException {
    for (int i = 0; i < m_longData.length; i++) {
      serialiser.writeLong(out, m_longData[i]);
    }

    for (int i = 0; i < m_doubleData.length; i++) {
      serialiser.writeDouble(out, m_doubleData[i]);
    }
  }

  /**
   * Efficient externalisation method used by {@link
   * TestStatisticsFactory#readStatisticsExternal}.
   *
   * @param in Handle to the input stream.
   * @param serialiser <code>Serialiser</code> helper object.
   * @exception IOException If an error occurs.
   */
  protected RawStatisticsImplementation(ObjectInput in, Serialiser serialiser)
    throws IOException {
    this();

    for (int i = 0; i < m_longData.length; i++) {
      m_longData[i] = serialiser.readLong(in);
    }

    for (int i = 0; i < m_doubleData.length; i++) {
      m_doubleData[i] = serialiser.readDouble(in);
    }
  }
}
