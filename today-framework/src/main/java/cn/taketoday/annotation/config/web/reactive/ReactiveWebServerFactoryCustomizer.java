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

package cn.taketoday.annotation.config.web.reactive;

import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.framework.web.reactive.server.ConfigurableReactiveWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.PropertyMapper;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link ServerProperties} to reactive
 * servers.
 *
 * @author Brian Clozel
 * @author Yunkun Huang
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 11:40
 */
public class ReactiveWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableReactiveWebServerFactory>, Ordered {

  @Nullable
  private final SslBundles sslBundles;

  @Nullable
  private final ApplicationTemp applicationTemp;

  private final ServerProperties serverProperties;

  /**
   * Create a new {@link ReactiveWebServerFactoryCustomizer} instance.
   *
   * @param serverProperties the server properties
   */
  public ReactiveWebServerFactoryCustomizer(ServerProperties serverProperties, @Nullable SslBundles sslBundles) {
    this(serverProperties, sslBundles, null);
  }

  /**
   * Create a new {@link ReactiveWebServerFactoryCustomizer} instance.
   *
   * @param serverProperties the server properties
   * @param sslBundles the SSL bundles
   */
  public ReactiveWebServerFactoryCustomizer(ServerProperties serverProperties,
          @Nullable SslBundles sslBundles, @Nullable ApplicationTemp applicationTemp) {
    this.serverProperties = serverProperties;
    this.sslBundles = sslBundles;
    this.applicationTemp = applicationTemp;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public void customize(ConfigurableReactiveWebServerFactory factory) {
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(sslBundles).to(factory::setSslBundles);
    map.from(applicationTemp).to(factory::setApplicationTemp);

    map.from(serverProperties::getSsl).to(factory::setSsl);
    map.from(serverProperties::getPort).to(factory::setPort);
    map.from(serverProperties::getHttp2).to(factory::setHttp2);
    map.from(serverProperties::getAddress).to(factory::setAddress);
    map.from(serverProperties.getShutdown()).to(factory::setShutdown);
    map.from(serverProperties::getCompression).to(factory::setCompression);
  }

}
