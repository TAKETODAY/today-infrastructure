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

package infra.annotation.config.web.embedded;

import infra.annotation.ConditionalOnWebApplication;
import infra.annotation.ConditionalOnWebApplication.Type;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.env.Environment;
import infra.stereotype.Component;
import infra.web.server.ServerProperties;
import reactor.netty.http.server.HttpServer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for embedded servlet and reactive
 * web servers customizations.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@DisableDIAutoConfiguration
@ConditionalOnWebApplication(type = Type.REACTIVE)
@EnableConfigurationProperties(ServerProperties.class)
public class EmbeddedWebServerFactoryCustomizerAutoConfiguration {

  /**
   * Nested configuration if Netty is being used.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(HttpServer.class)
  public static class NettyWebServerFactoryCustomizerConfiguration {

    @Component
    static ReactorNettyWebServerFactoryCustomizer nettyWebServerFactoryCustomizer(
            Environment environment, ServerProperties serverProperties) {
      return new ReactorNettyWebServerFactoryCustomizer(environment, serverProperties);
    }

  }

}
