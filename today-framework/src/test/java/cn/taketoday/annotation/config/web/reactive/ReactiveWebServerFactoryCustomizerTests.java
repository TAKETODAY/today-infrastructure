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

package cn.taketoday.annotation.config.web.reactive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import cn.taketoday.core.ssl.DefaultSslBundleRegistry;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.framework.web.reactive.server.ConfigurableReactiveWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.Shutdown;
import cn.taketoday.framework.web.server.Ssl;

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
    this.properties.ssl = (ssl);
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
