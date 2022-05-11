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

import java.util.Collections;
import java.util.List;

import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.framework.web.servlet.WebListenerRegistrar;
import cn.taketoday.framework.web.servlet.server.ConfigurableServletWebServerFactory;
import cn.taketoday.framework.web.servlet.server.CookieSameSiteSupplier;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.PropertyMapper;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link ServerProperties} and
 * {@link WebListenerRegistrar WebListenerRegistrars} to servlet web servers.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Olivier Lamy
 * @author Yunkun Huang
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/27 21:57
 */
public class ServletWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>, Ordered {

  private final ServerProperties serverProperties;

  private final List<WebListenerRegistrar> webListenerRegistrars;

  private final List<CookieSameSiteSupplier> cookieSameSiteSuppliers;

  public ServletWebServerFactoryCustomizer(ServerProperties serverProperties) {
    this(serverProperties, Collections.emptyList());
  }

  public ServletWebServerFactoryCustomizer(ServerProperties serverProperties,
          List<WebListenerRegistrar> webListenerRegistrars) {
    this(serverProperties, webListenerRegistrars, null);
  }

  ServletWebServerFactoryCustomizer(ServerProperties serverProperties,
          List<WebListenerRegistrar> webListenerRegistrars, List<CookieSameSiteSupplier> cookieSameSiteSuppliers) {
    this.serverProperties = serverProperties;
    this.webListenerRegistrars = webListenerRegistrars;
    this.cookieSameSiteSuppliers = cookieSameSiteSuppliers;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public void customize(ConfigurableServletWebServerFactory factory) {
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(serverProperties::getPort).to(factory::setPort);
    map.from(serverProperties::getSession).to(factory::setSession);
    map.from(serverProperties::getAddress).to(factory::setAddress);

    map.from(serverProperties.getServlet()::getContextPath).to(factory::setContextPath);
    map.from(serverProperties.getServlet()::getApplicationDisplayName).to(factory::setDisplayName);
    map.from(serverProperties.getServlet()::isRegisterDefaultServlet).to(factory::setRegisterDefaultServlet);
    map.from(serverProperties.getServlet()::getJsp).to(factory::setJsp);
    map.from(serverProperties.getServlet()::getContextParameters).to(factory::setInitParameters);

    map.from(serverProperties::getSsl).to(factory::setSsl);
    map.from(serverProperties::getCompression).to(factory::setCompression);
    map.from(serverProperties::getHttp2).to(factory::setHttp2);
    map.from(serverProperties::getServerHeader).to(factory::setServerHeader);
    map.from(serverProperties.getShutdown()).to(factory::setShutdown);
    for (WebListenerRegistrar registrar : webListenerRegistrars) {
      registrar.register(factory);
    }
    if (CollectionUtils.isNotEmpty(cookieSameSiteSuppliers)) {
      factory.setCookieSameSiteSuppliers(cookieSameSiteSuppliers);
    }
  }

}
