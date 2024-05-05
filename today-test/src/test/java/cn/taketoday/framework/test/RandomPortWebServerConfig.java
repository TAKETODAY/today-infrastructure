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

package cn.taketoday.framework.test;

import cn.taketoday.annotation.config.web.ErrorMvcAutoConfiguration;
import cn.taketoday.annotation.config.web.WebMvcAutoConfiguration;
import cn.taketoday.annotation.config.web.netty.NettyWebServerFactoryAutoConfiguration;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.web.server.support.NettyWebServerFactory;
import cn.taketoday.web.server.WebServerFactoryCustomizer;
import cn.taketoday.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import cn.taketoday.stereotype.Component;

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