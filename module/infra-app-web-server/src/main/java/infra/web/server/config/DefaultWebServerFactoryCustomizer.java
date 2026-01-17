/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.server.config;

import org.jspecify.annotations.Nullable;

import infra.core.ApplicationTemp;
import infra.core.Ordered;
import infra.app.ssl.SslBundles;
import infra.web.server.ConfigurableWebServerFactory;
import infra.web.server.WebServerFactoryCustomizer;

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
public class DefaultWebServerFactoryCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory>, Ordered {

  @Nullable
  private final SslBundles sslBundles;

  @Nullable
  private final ApplicationTemp applicationTemp;

  private final ServerProperties serverProperties;

  /**
   * Create a new {@link DefaultWebServerFactoryCustomizer} instance.
   *
   * @param serverProperties the server properties
   * @param sslBundles the SSL bundles
   */
  public DefaultWebServerFactoryCustomizer(ServerProperties serverProperties,
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
  public void customize(ConfigurableWebServerFactory factory) {
    serverProperties.applyTo(factory, sslBundles, applicationTemp);
  }

}
