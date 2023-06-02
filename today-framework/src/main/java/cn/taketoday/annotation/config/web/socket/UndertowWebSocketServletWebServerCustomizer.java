/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.web.socket;

import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import cn.taketoday.framework.web.embedded.undertow.UndertowServletWebServerFactory;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

/**
 * WebSocket customizer for {@link UndertowServletWebServerFactory}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UndertowWebSocketServletWebServerCustomizer
        implements WebServerFactoryCustomizer<UndertowServletWebServerFactory>, Ordered {

  @Override
  public void customize(UndertowServletWebServerFactory factory) {
    WebsocketDeploymentInfoCustomizer customizer = new WebsocketDeploymentInfoCustomizer();
    factory.addDeploymentInfoCustomizers(customizer);
  }

  @Override
  public int getOrder() {
    return 0;
  }

  private static class WebsocketDeploymentInfoCustomizer implements UndertowDeploymentInfoCustomizer {

    @Override
    public void customize(DeploymentInfo deploymentInfo) {
      WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
      deploymentInfo.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, info);
    }

  }

}
