// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004 Philip Aston
// Copyright (C) 2004 Bertrand Ave
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

package net.grinder.plugin.http;

import HTTPClient.CookieModule;
import HTTPClient.DefaultAuthHandler;
import HTTPClient.HTTPConnection;

import net.grinder.common.GrinderException;
import net.grinder.common.SSLContextFactory;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginRegistry;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.PluginThreadListener;
import net.grinder.script.Grinder;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsView;


/**
 * HTTP plugin.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @author Bertrand Ave
 * @version $Revision$
 */
public class HTTPPlugin implements GrinderPlugin {

  private static final HTTPPlugin s_singleton = new HTTPPlugin();

  static {
    final PluginRegistry registry = PluginRegistry.getInstance();

    // Registry might be null for unit tests.
    if (registry != null) {
      try {
        registry.register(s_singleton);
      }
      catch (GrinderException e) {
        throw new ExceptionInInitializerError(e);
      }
    }
  }

  /**
   * Static package scope accessor for the initialised instance of the
   * Plugin.
   *
   * @return The plugin instance.
   */
  static final HTTPPlugin getPlugin() {
    return s_singleton;
  }

  private PluginProcessContext m_pluginProcessContext;
  private SSLContextFactory m_sslContextFactory;
  private StatisticsIndexMap.LongIndex m_responseStatusIndex;
  private StatisticsIndexMap.LongIndex m_responseLengthIndex;
  private StatisticsIndexMap.LongIndex m_responseErrorsIndex;
  private StatisticsIndexMap.LongIndex m_dnsTimeIndex;
  private StatisticsIndexMap.LongIndex m_connectionTimeIndex;
  private StatisticsIndexMap.LongIndex m_firstByteTimeIndex;

  final PluginProcessContext getPluginProcessContext() {
    return m_pluginProcessContext;
  }

  final StatisticsIndexMap.LongIndex getResponseStatusIndex() {
    return m_responseStatusIndex;
  }

  final StatisticsIndexMap.LongIndex getResponseLengthIndex() {
    return m_responseLengthIndex;
  }

  final StatisticsIndexMap.LongIndex getResponseErrorsIndex() {
    return m_responseErrorsIndex;
  }

  final StatisticsIndexMap.LongIndex getDnsTimeIndex() {
    return m_dnsTimeIndex;
  }

  final StatisticsIndexMap.LongIndex getConnnectionTimeIndex() {
    return m_connectionTimeIndex;
  }

  final StatisticsIndexMap.LongIndex getFirstByteTimeIndex() {
    return m_firstByteTimeIndex;
  }

  /**
   * Called by the PluginRegistry when the plugin is first registered.
   *
   * @param processContext The plugin process context.
   * @exception PluginException if an error occurs.
   */
  public void initialize(PluginProcessContext processContext)
    throws PluginException {

    m_pluginProcessContext = processContext;

    final Grinder.ScriptContext scriptContext =
      processContext.getScriptContext();

    m_sslContextFactory = scriptContext.getSSLControl();

    // Remove standard HTTPClient modules which we don't want. We load
    // HTTPClient modules dynamically as we don't have public access.
    try {
      // Don't want to retry requests.
      HTTPConnection.removeDefaultModule(
        Class.forName("HTTPClient.RetryModule"));
    }
    catch (ClassNotFoundException e) {
      throw new PluginException("Could not load HTTPClient modules", e);
    }

    // Turn off cookie permission checks.
    CookieModule.setCookiePolicyHandler(null);

    // Turn off authorisation UI.
    DefaultAuthHandler.setAuthorizationPrompter(null);

    // Register custom statistics.
    try {
      final StatisticsView detailsStatisticsView = new StatisticsView();
      detailsStatisticsView.add(
        new ExpressionView(
          "HTTP response code", "statistic.httpplugin.responseStatusKey",
          StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_STATUS_KEY));

      detailsStatisticsView.add(
        new ExpressionView(
          "HTTP response length", "statistic.httpplugin.responseLengthKey",
          StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_LENGTH_KEY));

      detailsStatisticsView.add(
        new ExpressionView(
          "HTTP response errors", "statistic.httpplugin.responseErrorsKey",
          StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_ERRORS_KEY));

      detailsStatisticsView.add(
        new ExpressionView(
          "Time to resolve host", "statistic.httpplugin.dnsTimeKey",
          StatisticsIndexMap.HTTP_PLUGIN_DNS_TIME_KEY));

      detailsStatisticsView.add(
        new ExpressionView(
          "Time to establish connection",
          "statistic.httpplugin.connectTimeKey",
          StatisticsIndexMap.HTTP_PLUGIN_CONNECT_TIME_KEY));

      detailsStatisticsView.add(
        new ExpressionView(
          "Time to first byte", "statistic.httpplugin.firstByteTimeKey",
          StatisticsIndexMap.HTTP_PLUGIN_FIRST_BYTE_TIME_KEY));

      scriptContext.registerDetailStatisticsView(detailsStatisticsView);

      final StatisticsView summaryStatisticsView = new StatisticsView();

      summaryStatisticsView.add(
        new ExpressionView(
          "Mean response length",
          "statistic.httpplugin.meanResponseLengthKey",
          "(/ " + StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_LENGTH_KEY +
          " (+ (count timedTests) untimedTests))"));

      summaryStatisticsView.add(
        new ExpressionView(
          "Response bytes per second",
          "statistic.httpplugin.responseBPSKey",
          "(* 1000 (/ " + StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_LENGTH_KEY +
          " period))"));

      summaryStatisticsView.add(
        new ExpressionView(
          "Response errors",
          "statistic.httpplugin.responseErrorsKey",
          StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_ERRORS_KEY));

      summaryStatisticsView.add(
        new ExpressionView(
          "Mean time to resolve host",
          "statistic.httpplugin.meanDnsTimeKey",
          "(/ " + StatisticsIndexMap.HTTP_PLUGIN_DNS_TIME_KEY +
          " (+ (count timedTests) untimedTests))"));

      summaryStatisticsView.add(
        new ExpressionView(
          "Mean time to establish connection",
          "statistic.httpplugin.meanConnectTimeKey",
          "(/ " + StatisticsIndexMap.HTTP_PLUGIN_CONNECT_TIME_KEY +
          " (+ (count timedTests) untimedTests))"));

      summaryStatisticsView.add(
        new ExpressionView(
          "Mean time to first byte",
          "statistic.httpplugin.meanFirstByteTimeKey",
          "(/ " + StatisticsIndexMap.HTTP_PLUGIN_FIRST_BYTE_TIME_KEY +
          " (+ (count timedTests) untimedTests))"));

      scriptContext.registerSummaryStatisticsView(summaryStatisticsView);

      final StatisticsIndexMap statisticsIndexMap =
        StatisticsIndexMap.getInstance();

      m_responseStatusIndex =
        statisticsIndexMap.getIndexForLong(
          StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_STATUS_KEY);

      m_responseLengthIndex =
        statisticsIndexMap.getIndexForLong(
          StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_LENGTH_KEY);

      m_responseErrorsIndex =
        statisticsIndexMap.getIndexForLong(
          StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_ERRORS_KEY);

      m_dnsTimeIndex =
        statisticsIndexMap.getIndexForLong(
          StatisticsIndexMap.HTTP_PLUGIN_DNS_TIME_KEY);

      m_connectionTimeIndex =
        statisticsIndexMap.getIndexForLong(
          StatisticsIndexMap.HTTP_PLUGIN_CONNECT_TIME_KEY);

      m_firstByteTimeIndex =
        statisticsIndexMap.getIndexForLong(
          StatisticsIndexMap.HTTP_PLUGIN_FIRST_BYTE_TIME_KEY);
    }
    catch (GrinderException e) {
      throw new PluginException("Could not register custom statistics", e);
    }
  }

  /**
   * Called by the engine to obtain a new PluginThreadListener.
   *
   * @param threadContext The plugin thread context.
   * @return The new plugin thread listener.
   * @exception PluginException if an error occurs.
   */
  public PluginThreadListener createThreadListener(
    PluginThreadContext threadContext) throws PluginException {

    return new HTTPPluginThreadState(threadContext, m_sslContextFactory);
  }
}
