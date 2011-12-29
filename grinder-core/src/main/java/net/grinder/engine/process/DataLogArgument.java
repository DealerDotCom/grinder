// Copyright (C) 2011 Philip Aston
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

package net.grinder.engine.process;

import net.grinder.common.Test;
import net.grinder.statistics.StatisticsSet;


/**
 * Named, type-safe access to the additional information the data logger places
 * in the SLF4J arguments parameter.
 *
 * @param <T> Data type.
 * @author Philip Aston
 */
public final class DataLogArgument<T> {

  private static int s_numberOfArguments;

  /**
   * Worker thread ID.
   */
  public static final DataLogArgument<Integer> THREAD_INDEX =
    new DataLogArgument<Integer>();

  /**
   * Run ID.
   */
  public static final DataLogArgument<Integer> RUN_INDEX =
    new DataLogArgument<Integer>();

  /**
   * Test.
   */
  public static final DataLogArgument<Test> TEST_INDEX =
    new DataLogArgument<Test>();

  /**
   * Time since execution started, in milliseconds.
   */
  public static final DataLogArgument<Long> START_TIME_INDEX =
    new DataLogArgument<Long>();

  /**
   * Statistic values.
   */
  public static final DataLogArgument<StatisticsSet> STATISTICS_INDEX =
    new DataLogArgument<StatisticsSet>();

  private final int m_index;

  private DataLogArgument() {
    m_index = s_numberOfArguments++;
  }

  void put(Object[] array, T value) {
    array[m_index] = value;
  }

  /**
   * Get the value.
   *
   * @param arguments The arguments.
   * @return The value.
   */
  @SuppressWarnings("unchecked")
  public T get(Object[] arguments) {
    return (T) arguments[m_index];
  }

  static Object[] createArray() {
    return new Object[s_numberOfArguments];
  }
}
