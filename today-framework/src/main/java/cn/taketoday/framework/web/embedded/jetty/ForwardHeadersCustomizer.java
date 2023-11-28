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

package cn.taketoday.framework.web.embedded.jetty;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;

/**
 * {@link JettyServerCustomizer} to add {@link ForwardedRequestCustomizer}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ForwardHeadersCustomizer implements JettyServerCustomizer {

  @Override
  public void customize(Server server) {
    ForwardedRequestCustomizer customizer = new ForwardedRequestCustomizer();
    for (Connector connector : server.getConnectors()) {
      for (ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
        if (connectionFactory instanceof HttpConfiguration.ConnectionFactory factory) {
          factory.getHttpConfiguration().addCustomizer(customizer);
        }
      }
    }
  }

}
