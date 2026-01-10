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

package infra.web.server;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Map;

import infra.core.ApplicationTemp;
import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 13:11
 */
class AbstractConfigurableWebServerFactoryTests {

  @Test
  void defaultConstructorShouldSetDefaultPort() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    assertThat(factory.getPort()).isEqualTo(8080);
  }

  @Test
  void constructorWithPortShouldSetPort() {
    int port = 9090;
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory(port) { };

    assertThat(factory.getPort()).isEqualTo(port);
  }

  @Test
  void setPortShouldUpdatePort() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    factory.setPort(9090);
    assertThat(factory.getPort()).isEqualTo(9090);
  }

  @Test
  void getAddressShouldReturnNullByDefault() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    assertThat(factory.getAddress()).isNull();
  }

  @Test
  void setAddressShouldUpdateAddress() throws Exception {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    InetAddress address = InetAddress.getByName("127.0.0.1");

    factory.setAddress(address);
    assertThat(factory.getAddress()).isEqualTo(address);
  }

  @Test
  void getSslShouldReturnNullByDefault() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    assertThat(factory.getSsl()).isNull();
  }

  @Test
  void setSslShouldUpdateSsl() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    Ssl ssl = new Ssl();

    factory.setSsl(ssl);
    assertThat(factory.getSsl()).isEqualTo(ssl);
  }

  @Test
  void getSslBundlesShouldReturnNullByDefault() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    assertThat(factory.getSslBundles()).isNull();
  }

  @Test
  void setSslBundlesShouldUpdateSslBundles() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    SslBundles sslBundles = mock(SslBundles.class);

    factory.setSslBundles(sslBundles);
    assertThat(factory.getSslBundles()).isEqualTo(sslBundles);
  }

  @Test
  void getHttp2ShouldReturnNullByDefault() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    assertThat(factory.getHttp2()).isNull();
  }

  @Test
  void setHttp2ShouldUpdateHttp2() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    Http2 http2 = new Http2();

    factory.setHttp2(http2);
    assertThat(factory.getHttp2()).isEqualTo(http2);
  }

  @Test
  void getCompressionShouldReturnNullByDefault() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    assertThat(factory.getCompression()).isNull();
  }

  @Test
  void setCompressionShouldUpdateCompression() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    Compression compression = new Compression();

    factory.setCompression(compression);
    assertThat(factory.getCompression()).isEqualTo(compression);
  }

  @Test
  void getShutdownShouldReturnImmediateByDefault() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    assertThat(factory.getShutdown()).isEqualTo(Shutdown.IMMEDIATE);
  }

  @Test
  void setShutdownShouldUpdateShutdown() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    factory.setShutdown(Shutdown.GRACEFUL);
    assertThat(factory.getShutdown()).isEqualTo(Shutdown.GRACEFUL);
  }

  @Test
  void getApplicationTempShouldReturnDefaultInstance() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    assertThat(factory.getApplicationTemp()).isEqualTo(ApplicationTemp.instance);
  }

  @Test
  void setApplicationTempShouldUpdateApplicationTemp() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    ApplicationTemp applicationTemp = mock(ApplicationTemp.class);

    factory.setApplicationTemp(applicationTemp);
    assertThat(factory.getApplicationTemp()).isEqualTo(applicationTemp);
  }

  @Test
  void setApplicationTempShouldThrowExceptionWhenNull() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    assertThatThrownBy(() -> factory.setApplicationTemp(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ApplicationTemp is required");
  }

  @Test
  void isHttp2EnabledShouldReturnFalseWhenHttp2IsNull() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    assertThat(factory.isHttp2Enabled()).isFalse();
  }

  @Test
  void isHttp2EnabledShouldReturnFalseWhenHttp2IsDisabled() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    Http2 http2 = new Http2();
    http2.setEnabled(false);
    factory.setHttp2(http2);

    assertThat(factory.isHttp2Enabled()).isFalse();
  }

  @Test
  void isHttp2EnabledShouldReturnTrueWhenHttp2IsEnabled() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    Http2 http2 = new Http2();
    http2.setEnabled(true);
    factory.setHttp2(http2);

    assertThat(factory.isHttp2Enabled()).isTrue();
  }

  @Test
  void getSslBundleShouldThrowExceptionWhenSslIsNotEnabled() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    Ssl ssl = new Ssl();
    ssl.enabled = false;
    factory.setSsl(ssl);

    assertThatThrownBy(factory::getSslBundle)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("SSL is not enabled");
  }

  @Test
  void getServerNameSslBundlesShouldReturnEmptyMapWhenSslIsNull() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };

    Map<String, SslBundle> bundles = factory.getServerNameSslBundles();

    assertThat(bundles).isEmpty();
  }

  @Test
  void getServerNameSslBundlesShouldReturnEmptyMapWhenServerNameBundlesIsEmpty() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    Ssl ssl = new Ssl();
    factory.setSsl(ssl);

    Map<String, SslBundle> bundles = factory.getServerNameSslBundles();

    assertThat(bundles).isEmpty();
  }

  @Test
  void getServerNameSslBundlesShouldThrowExceptionWhenSslBundlesIsNull() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    Ssl ssl = new Ssl();
    Ssl.ServerNameSslBundle serverNameBundle = new Ssl.ServerNameSslBundle();
    serverNameBundle.setServerName("example.com");
    serverNameBundle.setBundle("test-bundle");
    ssl.serverNameBundles.add(serverNameBundle);
    factory.setSsl(ssl);
    factory.setSslBundles(null);

    assertThatThrownBy(factory::getServerNameSslBundles)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("sslBundles is required");
  }

  @Test
  void addBundleUpdateHandlerShouldNotThrowExceptionWhenSslBundlesIsNull() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    Ssl ssl = new Ssl();
    ssl.bundle = "test-bundle";

    assertThatNoException().isThrownBy(() ->
            factory.addBundleUpdateHandler(ssl, (serverName, sslBundle) -> { }));
  }

  @Test
  void addBundleUpdateHandlerShouldNotThrowExceptionWhenSslBundleNameIsNull() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    SslBundles sslBundles = mock(SslBundles.class);
    factory.setSslBundles(sslBundles);
    Ssl ssl = new Ssl();
    ssl.bundle = null;

    assertThatNoException().isThrownBy(() ->
            factory.addBundleUpdateHandler(ssl, (serverName, sslBundle) -> { }));
  }

  @Test
  void addBundleUpdateHandlerShouldNotThrowExceptionWhenSslBundleNameIsEmpty() {
    AbstractConfigurableWebServerFactory factory = new AbstractConfigurableWebServerFactory() { };
    SslBundles sslBundles = mock(SslBundles.class);
    factory.setSslBundles(sslBundles);
    Ssl ssl = new Ssl();
    ssl.bundle = "";

    assertThatNoException().isThrownBy(() ->
            factory.addBundleUpdateHandler(ssl, (serverName, sslBundle) -> { }));
  }

}