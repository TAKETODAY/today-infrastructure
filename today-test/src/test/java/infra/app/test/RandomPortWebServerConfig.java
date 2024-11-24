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

package infra.app.test;

import infra.annotation.config.web.ErrorMvcAutoConfiguration;
import infra.annotation.config.web.WebMvcAutoConfiguration;
import infra.annotation.config.web.netty.NettyWebServerFactoryAutoConfiguration;
import infra.context.annotation.Bean;
import infra.context.annotation.Import;
import infra.stereotype.Component;
import infra.web.server.WebServerFactoryCustomizer;
import infra.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import infra.web.server.support.NettyWebServerFactory;

@Import({ NettyWebServerFactoryAutoConfiguration.class, WebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
public class RandomPortWebServerConfig {

  @Bean
  static WebServerFactoryCustomizerBeanPostProcessor mockWebServerCustomizerBeanPostProcessor() {
    return new WebServerFactoryCustomizerBeanPostProcessor();
  }

  @Component
  static WebServerFactoryCustomizer<NettyWebServerFactory> randomPortCustomizer() {
    return server -> server.setPort(0);
  }

}