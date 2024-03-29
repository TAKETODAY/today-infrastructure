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

package cn.taketoday.annotation.config.web.servlet;

import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.util.CollectionUtils;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link ServerProperties} to Tomcat web
 * servers.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/27 21:55
 */
public class TomcatServletWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>, Ordered {

  private final ServerProperties serverProperties;

  public TomcatServletWebServerFactoryCustomizer(ServerProperties serverProperties) {
    this.serverProperties = serverProperties;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public void customize(TomcatServletWebServerFactory factory) {
    ServerProperties.Tomcat tomcatProperties = this.serverProperties.tomcat;
    if (CollectionUtils.isNotEmpty(tomcatProperties.additionalTldSkipPatterns)) {
      factory.getTldSkipPatterns().addAll(tomcatProperties.additionalTldSkipPatterns);
    }
    if (tomcatProperties.redirectContextRoot != null) {
      customizeRedirectContextRoot(factory, tomcatProperties.redirectContextRoot);
    }
    customizeUseRelativeRedirects(factory, tomcatProperties.useRelativeRedirects);
    factory.setDisableMBeanRegistry(!tomcatProperties.mbeanregistry.enabled);
  }

  private void customizeRedirectContextRoot(
          ConfigurableTomcatWebServerFactory factory, boolean redirectContextRoot) {
    factory.addContextCustomizers(context -> context.setMapperContextRootRedirectEnabled(redirectContextRoot));
  }

  private void customizeUseRelativeRedirects(
          ConfigurableTomcatWebServerFactory factory, boolean useRelativeRedirects) {
    factory.addContextCustomizers(context -> context.setUseRelativeRedirects(useRelativeRedirects));
  }

}
