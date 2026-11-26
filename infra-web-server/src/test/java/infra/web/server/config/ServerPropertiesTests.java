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

package infra.web.server.config;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.context.properties.source.MapConfigurationPropertySource;
import infra.core.ApplicationTemp;
import infra.web.server.Compression;
import infra.web.server.MockWebServerFactory;
import infra.web.server.Shutdown;
import infra.web.server.Ssl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerProperties}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Quinten De Swaef
 * @author Venil Noronha
 * @author Andrew McGhie
 * @author HaiTao Zhang
 * @author Rafiullah Hamedy
 * @author Chris Bono
 * @author Parviz Rozikov
 */
class ServerPropertiesTests {

  private final ServerProperties properties = new ServerProperties();

  @Test
  void testAddressBinding() throws Exception {
    bind("server.address", "127.0.0.1");
    assertThat(this.properties.address).isEqualTo(InetAddress.getByName("127.0.0.1"));
  }

  @Test
  void testPortBinding() {
    bind("server.port", "9000");
    assertThat(this.properties.port.intValue()).isEqualTo(9000);
  }

  @Test
  void applyToShouldApplyConfiguredValues() throws Exception {
    this.properties.port = 9000;
    this.properties.address = InetAddress.getByName("127.0.0.1");
    this.properties.ssl = new Ssl();
    this.properties.compression = new Compression();
    this.properties.shutdown = Shutdown.IMMEDIATE;
    MockWebServerFactory factory = new MockWebServerFactory();
    ApplicationTemp applicationTemp = ApplicationTemp.instance;

    this.properties.applyTo(factory, null, applicationTemp);

    assertThat(factory.getPort()).isEqualTo(9000);
    assertThat(factory.getAddress()).isEqualTo(this.properties.address);
    assertThat(factory.getSsl()).isSameAs(this.properties.ssl);
    assertThat(factory.getCompression()).isSameAs(this.properties.compression);
    assertThat(factory.getShutdown()).isEqualTo(Shutdown.IMMEDIATE);
    assertThat(factory.getApplicationTemp()).isSameAs(applicationTemp);
  }

  @Test
  void multipartTempRepositoryShouldUseExplicitBaseAndSubDirectory() {
    this.properties.multipart.tempBaseDir = "/tmp/uploads";
    this.properties.multipart.tempSubDir = "requests";

    assertThat(this.properties.multipart.computeTempRepository(null))
            .isEqualTo(Path.of("/tmp/uploads", "requests"));
  }

  @Test
  void multipartTempRepositoryShouldUseBaseDirectoryWithoutSubDirectory() {
    this.properties.multipart.tempBaseDir = "/tmp/uploads";

    assertThat(this.properties.multipart.computeTempRepository(null))
            .isEqualTo(Path.of("/tmp/uploads"));
  }

  @Test
  void multipartTempRepositoryShouldUseApplicationTempAndDefaultSubDirectory() {
    ApplicationTemp applicationTemp = ApplicationTemp.instance;

    assertThat(this.properties.multipart.computeTempRepository(applicationTemp))
            .isEqualTo(applicationTemp.getDir("multipart"));
  }

  @Test
  void multipartTempRepositoryShouldUseApplicationTempAndConfiguredSubDirectory() {
    this.properties.multipart.tempSubDir = "requests";
    ApplicationTemp applicationTemp = ApplicationTemp.instance;

    assertThat(this.properties.multipart.computeTempRepository(applicationTemp))
            .isEqualTo(applicationTemp.getDir("requests"));
  }

  @Test
  void multipartTempRepositoryShouldUseSingletonApplicationTempWhenNull() {
    assertThat(this.properties.multipart.computeTempRepository(null))
            .isEqualTo(ApplicationTemp.instance.getDir("multipart"));
  }

  private void bind(String name, String value) {
    bind(Collections.singletonMap(name, value));
  }

  private void bind(Map<String, String> map) {
    ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
    new Binder(source).bind("server", Bindable.ofInstance(this.properties));
  }

}
