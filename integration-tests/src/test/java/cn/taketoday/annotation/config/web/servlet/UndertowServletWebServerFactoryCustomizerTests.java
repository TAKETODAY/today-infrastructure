/*
 * Copyright 2012-2020 the original author or authors.
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

package cn.taketoday.annotation.config.web.servlet;

import org.junit.jupiter.api.Test;

import cn.taketoday.framework.web.embedded.undertow.UndertowServletWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UndertowServletWebServerFactoryCustomizer}
 *
 * @author Andy Wilkinson
 */
class UndertowServletWebServerFactoryCustomizerTests {

  @Test
  void eagerFilterInitCanBeDisabled() {
    UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory(0);
    assertThat(factory.isEagerFilterInit()).isTrue();
    ServerProperties serverProperties = new ServerProperties();
    serverProperties.getUndertow().setEagerFilterInit(false);
    new UndertowServletWebServerFactoryCustomizer(serverProperties).customize(factory);
    assertThat(factory.isEagerFilterInit()).isFalse();
  }

  @Test
  void preservePathOnForwardCanBeEnabled() {
    UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory(0);
    assertThat(factory.isPreservePathOnForward()).isFalse();
    ServerProperties serverProperties = new ServerProperties();
    serverProperties.getUndertow().setPreservePathOnForward(true);
    new UndertowServletWebServerFactoryCustomizer(serverProperties).customize(factory);
    assertThat(factory.isPreservePathOnForward()).isTrue();
  }

}
