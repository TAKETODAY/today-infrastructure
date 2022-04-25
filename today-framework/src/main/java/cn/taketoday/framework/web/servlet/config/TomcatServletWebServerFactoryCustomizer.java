/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.servlet.config;

import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.util.ObjectUtils;

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
    ServerProperties.Tomcat tomcatProperties = this.serverProperties.getTomcat();
    if (ObjectUtils.isNotEmpty(tomcatProperties.getAdditionalTldSkipPatterns())) {
      factory.getTldSkipPatterns().addAll(tomcatProperties.getAdditionalTldSkipPatterns());
    }
    if (tomcatProperties.getRedirectContextRoot() != null) {
      customizeRedirectContextRoot(factory, tomcatProperties.getRedirectContextRoot());
    }
    customizeUseRelativeRedirects(factory, tomcatProperties.isUseRelativeRedirects());
    factory.setDisableMBeanRegistry(!tomcatProperties.getMbeanregistry().isEnabled());
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
