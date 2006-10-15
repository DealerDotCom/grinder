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


/**
 * Provides references to commonly used {@link StatisticsView}s.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class CommonStatisticsViews {
  private final StatisticsView m_detailStatisticsView = new StatisticsView();

  private final StatisticsView m_summaryStatisticsView =
    new StatisticsView();

  CommonStatisticsViews(StatisticExpressionFactory expressionFactory) {
    try {
      final ExpressionView[] detailExpressionViews = {
        new ExpressionView("Test time",
                           "(sum timedTests)",
                           expressionFactory,
                           false),
        new ExpressionView("Errors",
                           "errors",
                           expressionFactory,
                           false),
      };

      for (int i = 0; i < detailExpressionViews.length; ++i) {
        m_detailStatisticsView.add(detailExpressionViews[i]);
      }

      final ExpressionView[] summaryExpressionViews = {
        new ExpressionView("Tests",
                           "(+ (count timedTests) untimedTests)",
                           expressionFactory,
                           true),
        new ExpressionView("Errors",
                           "errors",
                           expressionFactory,
                           true),
        new ExpressionView("Mean Test Time (ms)",
                           "(/ (sum timedTests) (count timedTests))",
                           expressionFactory,
                           false),
        new ExpressionView("Test Time Standard Deviation (ms)",
                           "(sqrt (variance timedTests))",
                           expressionFactory,
                           false),
      };

      for (int i = 0; i < summaryExpressionViews.length; ++i) {
        m_summaryStatisticsView.add(summaryExpressionViews[i]);
      }
    }
    catch (StatisticsException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Get the detail {@link StatisticsView}.
   *
   * @return The {@link StatisticsView}.
   */
  public StatisticsView getDetailStatisticsView() {
    return m_detailStatisticsView;
  }

  /**
   * Get the summary {@link StatisticsView}.
   *
   * @return The {@link StatisticsView}.
   */
  public StatisticsView getSummaryStatisticsView() {
    return m_summaryStatisticsView;
  }
}
