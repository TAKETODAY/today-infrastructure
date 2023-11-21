/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core.ssl;

import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SslManagerBundle}.
 *
 * @author Phillip Webb
 */
class SslManagerBundleTests {

  private KeyManagerFactory keyManagerFactory = mock(KeyManagerFactory.class);

  private TrustManagerFactory trustManagerFactory = mock(TrustManagerFactory.class);

  @Test
  void getKeyManagersDelegatesToFactory() {
    SslManagerBundle bundle = SslManagerBundle.of(this.keyManagerFactory, this.trustManagerFactory);
    bundle.getKeyManagers();
    then(this.keyManagerFactory).should().getKeyManagers();
  }

  @Test
  void getTrustManagersDelegatesToFactory() {
    SslManagerBundle bundle = SslManagerBundle.of(this.keyManagerFactory, this.trustManagerFactory);
    bundle.getTrustManagers();
    then(this.trustManagerFactory).should().getTrustManagers();
  }

  @Test
  void createSslContextCreatesInitializedSslContext() {
    SslManagerBundle bundle = SslManagerBundle.of(this.keyManagerFactory, this.trustManagerFactory);
    SSLContext sslContext = bundle.createSslContext("TLS");
    assertThat(sslContext).isNotNull();
    assertThat(sslContext.getProtocol()).isEqualTo("TLS");
  }

  @Test
  void ofWhenKeyManagerFactoryIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> SslManagerBundle.of(null, this.trustManagerFactory))
            .withMessage("KeyManagerFactory is required");
  }

  @Test
  void ofWhenTrustManagerFactoryIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> SslManagerBundle.of(this.keyManagerFactory, null))
            .withMessage("TrustManagerFactory is required");
  }

  @Test
  void ofCreatesSslManagerBundle() {
    SslManagerBundle bundle = SslManagerBundle.of(this.keyManagerFactory, this.trustManagerFactory);
    assertThat(bundle.getKeyManagerFactory()).isSameAs(this.keyManagerFactory);
    assertThat(bundle.getTrustManagerFactory()).isSameAs(this.trustManagerFactory);
  }

  @Test
  void fromCreatesDefaultSslManagerBundle() {
    SslManagerBundle bundle = SslManagerBundle.from(SslStoreBundle.NONE, SslBundleKey.NONE);
    assertThat(bundle).isInstanceOf(DefaultSslManagerBundle.class);
  }

}
