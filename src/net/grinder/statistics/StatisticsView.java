// Copyright (C) 2000, 2001, 2002, 2003 Philip Aston
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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Comparator;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.grinder.common.GrinderException;


/**
 * An ordered collection of {@link ExpressionView}s.
 *
 * @author Philip Aston
 * @version $Revision$
 * @see net.grinder.script.ScriptContext#registerDetailStatisticsView
 * @see net.grinder.script.ScriptContext#registerSummaryStatisticsView
 **/
public final class StatisticsView implements Externalizable {

  private static final long serialVersionUID = -4846650473903375223L;

  /**
   * We define a <code>Comparator</code> for {@link ExpressionView}s
   * rather than having the <code>ExpressionView</code> implement
   * <code>Comparable</code> because our sort order is inconsistent with equals.
   **/
  private static final Comparator s_expressionViewComparator =
    new Comparator() {
      public final int compare(Object a, Object b) {
	final ExpressionView viewA = (ExpressionView)a;
	final ExpressionView viewB = (ExpressionView)b;

	if (viewA.getCreationOrder() < viewB.getCreationOrder()) {
	  return -1;
	}
	else if (viewA.getCreationOrder() > viewB.getCreationOrder()) {
	  return 1;
	}
	else {
	  // Should assert ? Same creation order => same instance.
	  return 0;
	}
      }
    };

  static {
    // Ensure that the standard ExpressionViews are initialised
    // before any user ExpressionViews.
    Class dummy = CommonStatisticsViews.class;
  }

  /**
   * We use this set to ensure that new views are unique. We can't
   * do this with a SortedSet because our sort order is inconsistent
   * with equals.
   **/
  private final transient Set m_unique = new HashSet();

  /**
   * @link aggregation
   * @associates <{net.grinder.statistics.ExpressionView}>
   * @supplierCardinality 0..*
   **/
  private final SortedSet m_columns;

  /**
   * Creates a new <code>StatisticsView</code> instance.
   **/
  public StatisticsView() {
    m_columns = new TreeSet(s_expressionViewComparator);
  }

  /**
   * Add all the {@link ExpressionView}s in <code>other</code> to
   * this <code>StatisticsView</code>.
   *
   * @param other Another <code>StatisticsView</code>.
   **/
  public final synchronized void add(StatisticsView other) {
    final Iterator iterator = other.m_columns.iterator();

    while (iterator.hasNext()) {
      add((ExpressionView)iterator.next());
    }
  }

  /**
   * Add the specified {@link ExpressionView} to this
   * <code>StatisticsView</code>.
   *
   * @param statistic An {@link ExpressionView}.
   **/
  public final synchronized void add(ExpressionView statistic) {
    if (!m_unique.contains(statistic)) {
      m_unique.add(statistic);
      m_columns.add(statistic);
    }
  }

  /**
   * Return our {@link ExpressionView}s as an array.
   *
   * @return The {@link ExpressionView}s.
   **/
  public final synchronized ExpressionView[] getExpressionViews() {
    return (ExpressionView[])m_columns.toArray(new ExpressionView[0]);
  }

  /**
   * Externalisation method.
   *
   * @param out Handle to the output stream.
   * @exception IOException If an I/O error occurs.
   **/
  public synchronized void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(m_columns.size());

    final Iterator iterator = m_columns.iterator();

    while (iterator.hasNext()) {
      final ExpressionView view = (ExpressionView)iterator.next();
      view.myWriteExternal(out);
    }
  }

  /**
   * Externalisation method.
   *
   * @param in Handle to the input stream.
   * @exception IOException If an I/O error occurs.
   **/
  public synchronized void readExternal(ObjectInput in) throws IOException {
    final int n = in.readInt();

    m_columns.clear();

    for (int i=0; i<n; i++) {
      try {
	add(new ExpressionView(in));
      }
      catch (GrinderException e) {
	throw new IOException(
	  "Could not instantiate ExpressionView: " + e.getMessage());
      }
    }
  }
}
