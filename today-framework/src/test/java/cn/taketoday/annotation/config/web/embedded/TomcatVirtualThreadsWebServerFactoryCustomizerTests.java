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

import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.util.function.Consumer;

import cn.taketoday.framework.web.embedded.tomcat.TomcatServletWebServerFactory;
import cn.taketoday.framework.web.embedded.tomcat.TomcatWebServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/1 23:36
 */
class TomcatVirtualThreadsWebServerFactoryCustomizerTests {

  private final TomcatVirtualThreadsWebServerFactoryCustomizer customizer = new TomcatVirtualThreadsWebServerFactoryCustomizer();

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void shouldSetVirtualThreadExecutor() {
    withWebServer((webServer) -> assertThat(webServer.getTomcat().getConnector().getProtocolHandler().getExecutor())
            .isInstanceOf(VirtualThreadExecutor.class));
  }

  private TomcatWebServer getWebServer() {
    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
    this.customizer.customize(factory);
    return (TomcatWebServer) factory.getWebServer();
  }

  private void withWebServer(Consumer<TomcatWebServer> callback) {
    TomcatWebServer webServer = getWebServer();
    webServer.start();
    try {
      callback.accept(webServer);
    }
    finally {
      webServer.stop();
    }
  }

}