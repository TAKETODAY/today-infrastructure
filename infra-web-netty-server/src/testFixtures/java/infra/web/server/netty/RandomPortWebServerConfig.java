/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.server.netty;

import infra.context.annotation.Bean;
import infra.context.annotation.Import;
import infra.stereotype.Component;
import infra.web.server.WebServerFactoryCustomizer;
import infra.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import infra.web.server.netty.config.NettyWebServerFactoryAutoConfiguration;
import infra.web.config.ErrorMvcAutoConfiguration;
import infra.web.config.WebMvcAutoConfiguration;

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
