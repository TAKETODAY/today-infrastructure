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

package cn.taketoday.annotation.config.web.servlet;

import cn.taketoday.framework.web.embedded.undertow.UndertowServletWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;

/**
 * {@link WebServerFactoryCustomizer} to apply {@link ServerProperties} to Undertow
 * Servlet web servers.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/27 21:52
 */
public class UndertowServletWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

  private final ServerProperties serverProperties;

  public UndertowServletWebServerFactoryCustomizer(ServerProperties serverProperties) {
    this.serverProperties = serverProperties;
  }

  @Override
  public void customize(UndertowServletWebServerFactory factory) {
    factory.setEagerFilterInit(this.serverProperties.getUndertow().isEagerFilterInit());
    factory.setPreservePathOnForward(this.serverProperties.getUndertow().isPreservePathOnForward());
  }

}
