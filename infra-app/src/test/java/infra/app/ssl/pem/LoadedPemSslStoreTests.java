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

package infra.app.ssl.pem;

import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;

import infra.core.io.DefaultResourceLoader;
import infra.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;

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
    LoadedPemSslStore store = new LoadedPemSslStore(details, new DefaultResourceLoader());
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(store::certificates);
  }

  @Test
  void privateKeyIsLoadedLazily() {
    PemSslStoreDetails details = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
            .withPrivateKey("classpath:missing-test-key.pem");
    LoadedPemSslStore store = new LoadedPemSslStore(details, new DefaultResourceLoader());
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(store::privateKey);
  }

  @Test
  void withAliasIsLazy() {
    PemSslStoreDetails details = PemSslStoreDetails.forCertificate("classpath:missing-test-cert.pem")
            .withPrivateKey("classpath:test-key.pem");
    PemSslStore store = new LoadedPemSslStore(details, new DefaultResourceLoader()).withAlias("alias");
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(store::certificates);
  }

  @Test
  void withPasswordIsLazy() {
    PemSslStoreDetails details = PemSslStoreDetails.forCertificate("classpath:missing-test-cert.pem")
            .withPrivateKey("classpath:test-key.pem");
    PemSslStore store = new LoadedPemSslStore(details, new DefaultResourceLoader()).withPassword("password");
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(store::certificates);
  }

  @Test
  void usesResourceLoader() {
    PemSslStoreDetails details = PemSslStoreDetails.forCertificate("classpath:test-cert.pem");
    ResourceLoader resourceLoader = spy(new DefaultResourceLoader());
    LoadedPemSslStore store = new LoadedPemSslStore(details, resourceLoader);
    store.certificates();
    then(resourceLoader).should(atLeastOnce()).getResource("classpath:test-cert.pem");
  }

}
