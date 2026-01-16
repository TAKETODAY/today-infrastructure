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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 13:03
 */
class SslTests {

  @Test
  void isEnabledReturnsFalseWhenSslIsNull() {
    assertThat(Ssl.isEnabled(null)).isFalse();
  }

  @Test
  void isEnabledReturnsFalseWhenSslIsDisabled() {
    Ssl ssl = new Ssl();
    ssl.enabled = false;

    assertThat(Ssl.isEnabled(ssl)).isFalse();
  }

  @Test
  void isEnabledReturnsTrueWhenSslIsEnabled() {
    Ssl ssl = new Ssl();
    ssl.enabled = true;

    assertThat(Ssl.isEnabled(ssl)).isTrue();
  }

  @Test
  void forBundleCreatesSslInstanceWithBundleName() {
    String bundleName = "test-bundle";

    Ssl ssl = Ssl.forBundle(bundleName);

    assertThat(ssl).isNotNull();
    assertThat(ssl.bundle).isEqualTo(bundleName);
    assertThat(ssl.enabled).isTrue();
  }

  @Test
  void forBundleWithNullBundleName() {
    Ssl ssl = Ssl.forBundle(null);

    assertThat(ssl).isNotNull();
    assertThat(ssl.bundle).isNull();
    assertThat(ssl.enabled).isTrue();
  }

  @Test
  void clientAuthMapReturnsCorrectValues() {
    String noneValue = "none";
    String wantValue = "want";
    String needValue = "need";

    String result1 = Ssl.ClientAuth.map(Ssl.ClientAuth.NONE, noneValue, wantValue, needValue);
    String result2 = Ssl.ClientAuth.map(Ssl.ClientAuth.WANT, noneValue, wantValue, needValue);
    String result3 = Ssl.ClientAuth.map(Ssl.ClientAuth.NEED, noneValue, wantValue, needValue);
    String result4 = Ssl.ClientAuth.map(null, noneValue, wantValue, needValue);

    assertThat(result1).isEqualTo(noneValue);
    assertThat(result2).isEqualTo(wantValue);
    assertThat(result3).isEqualTo(needValue);
    assertThat(result4).isEqualTo(noneValue);
  }

  @Test
  void defaultValuesAreCorrect() {
    Ssl ssl = new Ssl();

    assertThat(ssl.enabled).isTrue();
    assertThat(ssl.bundle).isNull();
    assertThat(ssl.keyAlias).isNull();
    assertThat(ssl.keyStore).isNull();
    assertThat(ssl.keyStorePassword).isNull();
    assertThat(ssl.keyStoreType).isNull();
    assertThat(ssl.keyStoreProvider).isNull();
    assertThat(ssl.trustStore).isNull();
    assertThat(ssl.trustStorePassword).isNull();
    assertThat(ssl.trustStoreType).isNull();
    assertThat(ssl.trustStoreProvider).isNull();
    assertThat(ssl.certificate).isNull();
    assertThat(ssl.certificatePrivateKey).isNull();
    assertThat(ssl.trustCertificate).isNull();
    assertThat(ssl.trustCertificatePrivateKey).isNull();
    assertThat(ssl.protocol).isEqualTo("TLS");
    assertThat(ssl.keyPassword).isNull();
    assertThat(ssl.ciphers).isNull();
    assertThat(ssl.enabledProtocols).isNull();
    assertThat(ssl.clientAuth).isNull();
    assertThat(ssl.handshakeTimeout).isEqualTo(Duration.ofSeconds(10));
    assertThat(ssl.serverNameBundles).isEmpty();
  }

  @Test
  void serverNameSslBundleGettersAndSettersWorkCorrectly() {
    Ssl.ServerNameSslBundle serverNameBundle = new Ssl.ServerNameSslBundle();
    String serverName = "example.com";
    String bundle = "test-bundle";

    serverNameBundle.setServerName(serverName);
    serverNameBundle.setBundle(bundle);

    assertThat(serverNameBundle.getServerName()).isEqualTo(serverName);
    assertThat(serverNameBundle.getBundle()).isEqualTo(bundle);
  }

}