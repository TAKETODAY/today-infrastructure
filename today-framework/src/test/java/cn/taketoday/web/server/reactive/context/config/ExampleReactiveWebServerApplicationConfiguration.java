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

package cn.taketoday.web.server.reactive.context.config;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.web.server.reactive.server.MockReactiveWebServerFactory;
import cn.taketoday.http.server.reactive.HttpHandler;

import static org.mockito.Mockito.mock;

/**
 * Example {@code @Configuration} for use with
 * {@code AnnotationConfigReactiveWebServerApplicationContextTests}.
 *
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
public class ExampleReactiveWebServerApplicationConfiguration {

  @Bean
  public MockReactiveWebServerFactory webServerFactory() {
    return new MockReactiveWebServerFactory();
  }

  @Bean
  public HttpHandler httpHandler() {
    return mock(HttpHandler.class);
  }

}
