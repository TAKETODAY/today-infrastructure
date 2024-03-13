/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.web.embedded;

import org.eclipse.jetty.util.VirtualThreads;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.embedded.jetty.ConfigurableJettyWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.lang.Assert;

/**
 * Activates virtual threads on the {@link ConfigurableJettyWebServerFactory}.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JettyVirtualThreadsWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory>, Ordered {

  private final ServerProperties serverProperties;

  public JettyVirtualThreadsWebServerFactoryCustomizer(ServerProperties serverProperties) {
    this.serverProperties = serverProperties;
  }

  @Override
  public void customize(ConfigurableJettyWebServerFactory factory) {
    Assert.state(VirtualThreads.areSupported(), "Virtual threads are not supported");
    QueuedThreadPool threadPool = JettyThreadPool.create(this.serverProperties.jetty.getThreads());
    threadPool.setVirtualThreadsExecutor(VirtualThreads.getDefaultVirtualThreadsExecutor());
    factory.setThreadPool(threadPool);
  }

  @Override
  public int getOrder() {
    return JettyWebServerFactoryCustomizer.ORDER + 1;
  }

}
