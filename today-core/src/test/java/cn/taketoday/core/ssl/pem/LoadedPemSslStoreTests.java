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

import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link LoadedPemSslStore}.
 *
 * @author Phillip Webb
 */
class LoadedPemSslStoreTests {

  @Test
  void certificatesAreLoadedLazily() {
    PemSslStoreDetails details = PemSslStoreDetails.forCertificate("classpath:missing-test-cert.pem")
            .withPrivateKey("classpath:test-key.pem");
    LoadedPemSslStore store = new LoadedPemSslStore(details);
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(store::certificates);
  }

  @Test
  void privateKeyIsLoadedLazily() {
    PemSslStoreDetails details = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
            .withPrivateKey("classpath:missing-test-key.pem");
    LoadedPemSslStore store = new LoadedPemSslStore(details);
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(store::privateKey);
  }

}
