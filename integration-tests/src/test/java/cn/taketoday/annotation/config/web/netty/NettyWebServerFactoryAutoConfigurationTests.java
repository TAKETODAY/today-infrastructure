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

package cn.taketoday.annotation.config.web.netty;

import org.junit.jupiter.api.Test;

import cn.taketoday.annotation.config.web.WebMvcAutoConfiguration;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.web.context.AnnotationConfigWebServerApplicationContext;
import cn.taketoday.framework.web.netty.NettyChannelInitializer;
import cn.taketoday.framework.web.netty.NettyWebServerFactory;
import cn.taketoday.framework.web.netty.StandardNettyWebEnvironment;
import cn.taketoday.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/5 21:03
 */
class NettyWebServerFactoryAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner(this::createContext)
          .withConfiguration(AutoConfigurations.of(
                  NettyWebServerFactoryAutoConfiguration.class, WebMvcAutoConfiguration.class))
          .withUserConfiguration(WebServerConfiguration.class);

  AnnotationConfigWebServerApplicationContext createContext() {
    var context = new AnnotationConfigWebServerApplicationContext();
    context.setEnvironment(new StandardNettyWebEnvironment());
    return context;
  }

  @Test
  void webServerFactory() {
    contextRunner.run(context -> {
      NettyWebServerFactory factory = context.getBean(NettyWebServerFactory.class);
      assertThat(factory.getWorkThreadCount()).isEqualTo(100);
    });
  }

  @Configuration(proxyBeanMethods = false)
  static class WebServerConfiguration {

    @Component
    NettyWebServerFactory webServerFactory(NettyChannelInitializer nettyChannelInitializer) {
      NettyWebServerFactory factory = new NettyWebServerFactory();
      factory.setWorkThreadCount(100);
      factory.setNettyChannelInitializer(nettyChannelInitializer);
      return factory;
    }

  }
}