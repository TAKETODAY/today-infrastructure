/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.web.socket;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.ee10.websocket.jakarta.server.JakartaWebSocketServerContainer;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.ee10.websocket.servlet.WebSocketUpgradeFilter;
import org.eclipse.jetty.websocket.core.server.WebSocketMappings;
import org.eclipse.jetty.websocket.core.server.WebSocketServerComponents;

import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.embedded.jetty.EmptyBuilderConfiguration;
import cn.taketoday.framework.web.embedded.jetty.JettyServletWebServerFactory;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;

/**
 * WebSocket customizer for {@link JettyServletWebServerFactory}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JettyWebSocketServletWebServerCustomizer
        implements WebServerFactoryCustomizer<JettyServletWebServerFactory>, Ordered {

  @Override
  public void customize(JettyServletWebServerFactory factory) {
    factory.addConfigurations(new EmptyBuilderConfiguration() {

      @Override
      public void configure(WebAppContext context) {
        ServletContextHandler contextHandler = context.getContext().getServletContextHandler();
        if (JettyWebSocketServerContainer.getContainer(context.getServletContext()) == null) {
          WebSocketServerComponents.ensureWebSocketComponents(context.getServer(), contextHandler);
          JettyWebSocketServerContainer.ensureContainer(context.getServletContext());
        }

        if (JakartaWebSocketServerContainer.getContainer(context.getServletContext()) == null) {
          WebSocketServerComponents.ensureWebSocketComponents(context.getServer(), contextHandler);
          WebSocketUpgradeFilter.ensureFilter(context.getServletContext());
          WebSocketMappings.ensureMappings(contextHandler);
          JakartaWebSocketServerContainer.ensureContainer(context.getServletContext());
        }
      }

    });
  }

  @Override
  public int getOrder() {
    return 0;
  }

}
