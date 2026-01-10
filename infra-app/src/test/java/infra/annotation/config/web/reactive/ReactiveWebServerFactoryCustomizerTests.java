/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.annotation.config.web.reactive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import infra.core.ssl.DefaultSslBundleRegistry;
import infra.core.ssl.SslBundles;
import infra.test.util.ReflectionTestUtils;
import infra.web.server.ServerProperties;
import infra.web.server.Shutdown;
import infra.web.server.Ssl;
import infra.web.server.reactive.ConfigurableReactiveWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveWebServerFactoryCustomizer}.
 *
 * @author Brian Clozel
 * @author Yunkun Huang
 */
class ReactiveWebServerFactoryCustomizerTests {

  private final ServerProperties properties = new ServerProperties();

  private final SslBundles sslBundles = new DefaultSslBundleRegistry();

  private ReactiveWebServerFactoryCustomizer customizer;

  @BeforeEach
  void setup() {
    this.customizer = new ReactiveWebServerFactoryCustomizer(this.properties, this.sslBundles);
  }

  @Test
  void testCustomizeServerPort() {
    ConfigurableReactiveWebServerFactory factory = mock(ConfigurableReactiveWebServerFactory.class);
    this.properties.port = (9000);
    this.customizer.customize(factory);
    then(factory).should().setPort(9000);
  }

  @Test
  void testCustomizeServerAddress() {
    ConfigurableReactiveWebServerFactory factory = mock(ConfigurableReactiveWebServerFactory.class);
    InetAddress address = InetAddress.getLoopbackAddress();
    this.properties.address = (address);
    this.customizer.customize(factory);
    then(factory).should().setAddress(address);
  }

  @Test
  void testCustomizeServerSsl() {
    ConfigurableReactiveWebServerFactory factory = mock(ConfigurableReactiveWebServerFactory.class);
    Ssl ssl = mock(Ssl.class);
    ReflectionTestUtils.setField(properties, "ssl", ssl);
    this.customizer.customize(factory);
    then(factory).should().setSsl(ssl);
    then(factory).should().setSslBundles(this.sslBundles);
  }

  @Test
  void whenShutdownPropertyIsSetThenShutdownIsCustomized() {
    this.properties.shutdown = (Shutdown.GRACEFUL);
    ConfigurableReactiveWebServerFactory factory = mock(ConfigurableReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setShutdown(assertArg(shutdown -> assertThat(shutdown).isEqualTo(Shutdown.GRACEFUL)));
  }

}
