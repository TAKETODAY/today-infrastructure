/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.embedded.jetty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ThreadPool;

import cn.taketoday.framework.web.server.ConfigurableWebServerFactory;

/**
 * {@link ConfigurableWebServerFactory} for Jetty-specific features.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JettyServletWebServerFactory
 * @see JettyReactiveWebServerFactory
 * @since 4.0
 */
public interface ConfigurableJettyWebServerFactory extends ConfigurableWebServerFactory {

  /**
   * Set the number of acceptor threads to use.
   *
   * @param acceptors the number of acceptor threads to use
   */
  void setAcceptors(int acceptors);

  /**
   * Set the {@link ThreadPool} that should be used by the {@link Server}. If set to
   * {@code null} (default), the {@link Server} creates a {@link ThreadPool} implicitly.
   *
   * @param threadPool the ThreadPool to be used
   */
  void setThreadPool(ThreadPool threadPool);

  /**
   * Set the number of selector threads to use.
   *
   * @param selectors the number of selector threads to use
   */
  void setSelectors(int selectors);

  /**
   * Set if x-forward-* headers should be processed.
   *
   * @param useForwardHeaders if x-forward headers should be used
   */
  void setUseForwardHeaders(boolean useForwardHeaders);

  /**
   * Add {@link JettyServerCustomizer}s that will be applied to the {@link Server}
   * before it is started.
   *
   * @param customizers the customizers to add
   */
  void addServerCustomizers(JettyServerCustomizer... customizers);

  /**
   * Sets the maximum number of concurrent connections.
   *
   * @param maxConnections the maximum number of concurrent connections
   */
  void setMaxConnections(int maxConnections);

}
