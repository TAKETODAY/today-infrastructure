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

package cn.taketoday.annotation.config.web.reactive;

import cn.taketoday.framework.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link ServerProperties} to Tomcat reactive
 * web servers.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 12:15
 */
public class TomcatReactiveWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> {

  private final ServerProperties serverProperties;

  public TomcatReactiveWebServerFactoryCustomizer(ServerProperties serverProperties) {
    this.serverProperties = serverProperties;
  }

  @Override
  public void customize(TomcatReactiveWebServerFactory factory) {
    factory.setDisableMBeanRegistry(!serverProperties.tomcat.getMbeanregistry().isEnabled());
  }

}
