/*
 * Copyright 2012-2022 the original author or authors.
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

package cn.taketoday.annotation.config.web.reactive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetAddress;

import cn.taketoday.framework.web.reactive.server.ConfigurableReactiveWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.Shutdown;
import cn.taketoday.framework.web.server.Ssl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveWebServerFactoryCustomizer}.
 *
 * @author Brian Clozel
 * @author Yunkun Huang
 */
class ReactiveWebServerFactoryCustomizerTests {

  private ServerProperties properties = new ServerProperties();

  private ReactiveWebServerFactoryCustomizer customizer;

  @BeforeEach
  void setup() {
    this.customizer = new ReactiveWebServerFactoryCustomizer(this.properties);
  }

  @Test
  void testCustomizeServerPort() {
    ConfigurableReactiveWebServerFactory factory = mock(ConfigurableReactiveWebServerFactory.class);
    this.properties.setPort(9000);
    this.customizer.customize(factory);
    then(factory).should().setPort(9000);
  }

  @Test
  void testCustomizeServerAddress() {
    ConfigurableReactiveWebServerFactory factory = mock(ConfigurableReactiveWebServerFactory.class);
    InetAddress address = InetAddress.getLoopbackAddress();
    this.properties.setAddress(address);
    this.customizer.customize(factory);
    then(factory).should().setAddress(address);
  }

  @Test
  void testCustomizeServerSsl() {
    ConfigurableReactiveWebServerFactory factory = mock(ConfigurableReactiveWebServerFactory.class);
    Ssl ssl = mock(Ssl.class);
    this.properties.setSsl(ssl);
    this.customizer.customize(factory);
    then(factory).should().setSsl(ssl);
  }

  @Test
  void whenShutdownPropertyIsSetThenShutdownIsCustomized() {
    this.properties.setShutdown(Shutdown.GRACEFUL);
    ConfigurableReactiveWebServerFactory factory = mock(ConfigurableReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    ArgumentCaptor<Shutdown> shutdownCaptor = ArgumentCaptor.forClass(Shutdown.class);
    then(factory).should().setShutdown(shutdownCaptor.capture());
    assertThat(shutdownCaptor.getValue()).isEqualTo(Shutdown.GRACEFUL);
  }

}
