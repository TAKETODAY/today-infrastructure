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

package cn.taketoday.core.ssl.pem;

import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/25 14:41
 */
class PemSslStoreTests {

  @Test
  void withAliasReturnsStoreWithNewAlias() {
    List<X509Certificate> certificates = List.of(mock(X509Certificate.class));
    PrivateKey privateKey = mock(PrivateKey.class);
    PemSslStore store = PemSslStore.of("type", "alias", "secret", certificates, privateKey);
    assertThat(store.withAlias("newalias").alias()).isEqualTo("newalias");
  }

  @Test
  void withPasswordReturnsStoreWithNewPassword() {
    List<X509Certificate> certificates = List.of(mock(X509Certificate.class));
    PrivateKey privateKey = mock(PrivateKey.class);
    PemSslStore store = PemSslStore.of("type", "alias", "secret", certificates, privateKey);
    assertThat(store.withPassword("newsecret").password()).isEqualTo("newsecret");
  }

  @Test
  void ofWhenNullCertificatesThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> PemSslStore.of(null, null, null, null, null))
            .withMessage("Certificates must not be empty");
  }

  @Test
  void ofWhenEmptyCertificatesThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> PemSslStore.of(null, null, null, Collections.emptyList(), null))
            .withMessage("Certificates must not be empty");
  }

  @Test
  void ofReturnsPemSslStore() {
    List<X509Certificate> certificates = List.of(mock(X509Certificate.class));
    PrivateKey privateKey = mock(PrivateKey.class);
    PemSslStore store = PemSslStore.of("type", "alias", "password", certificates, privateKey);
    assertThat(store.type()).isEqualTo("type");
    assertThat(store.alias()).isEqualTo("alias");
    assertThat(store.password()).isEqualTo("password");
    assertThat(store.certificates()).isEqualTo(certificates);
    assertThat(store.privateKey()).isEqualTo(privateKey);
  }

}