/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.web.embedded;

import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;

import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;

/**
 * Activates {@link VirtualThreadExecutor} on {@link ProtocolHandler Tomcat's protocol
 * handler}.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class TomcatVirtualThreadsWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory>, Ordered {

  @Override
  public void customize(ConfigurableTomcatWebServerFactory factory) {
    factory.addProtocolHandlerCustomizers(
            (protocolHandler) -> protocolHandler.setExecutor(new VirtualThreadExecutor("tomcat-handler-")));
  }

  @Override
  public int getOrder() {
    return TomcatWebServerFactoryCustomizer.order + 1;
  }

}
