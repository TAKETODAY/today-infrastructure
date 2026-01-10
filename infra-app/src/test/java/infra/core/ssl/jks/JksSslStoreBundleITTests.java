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

package infra.core.ssl.jks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyStore;
import java.util.Base64;
import java.util.function.Consumer;

import infra.app.io.ApplicationResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.util.function.ThrowingConsumer;
import infra.web.MockPkcs11Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JksSslStoreBundle}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
@MockPkcs11Security
class JksSslStoreBundleITTests {

  @Test
  void whenLocationsAreBase64Encoded() throws IOException {
    JksSslStoreDetails keyStoreDetails = JksSslStoreDetails.forLocation(encodeFileContent("classpath:test.p12"))
            .withPassword("secret");
    JksSslStoreDetails trustStoreDetails = JksSslStoreDetails.forLocation(encodeFileContent("classpath:test.jks"))
            .withPassword("secret");
    JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("test-alias", "password"));
    assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("test-alias", "secret"));
  }

  @Test
  void invalidBase64EncodedLocationThrowsException() {
    JksSslStoreDetails keyStoreDetails = JksSslStoreDetails.forLocation("base64:not base 64");
    JksSslStoreBundle jksSslStoreBundle = new JksSslStoreBundle(keyStoreDetails, null);
    assertThatIllegalStateException().isThrownBy(jksSslStoreBundle::getKeyStore)
            .withMessageContaining("key store")
            .withMessageContaining("base64:not base 64")
            .havingRootCause()
            .isInstanceOf(IllegalArgumentException.class)
            .withMessageContaining("Illegal base64");
  }

  private Consumer<KeyStore> storeContainingCertAndKey(String keyAlias, String keyPassword) {
    return storeContainingCertAndKey(KeyStore.getDefaultType(), keyAlias, keyPassword);
  }

  private Consumer<KeyStore> storeContainingCertAndKey(String keyStoreType, String keyAlias, String keyPassword) {
    return ThrowingConsumer.of((keyStore) -> {
      assertThat(keyStore).isNotNull();
      assertThat(keyStore.getType()).isEqualTo(keyStoreType);
      assertThat(keyStore.containsAlias(keyAlias)).isTrue();
      assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
      assertThat(keyStore.getKey(keyAlias, keyPassword.toCharArray())).isNotNull();
    });
  }

  private String encodeFileContent(String location) throws IOException {
    ResourceLoader loader = ApplicationResourceLoader.of();
    Resource resource = loader.getResource(location);
    byte[] bytes = Files.readAllBytes(resource.getFile().toPath());
    return "base64:" + Base64.getEncoder().encodeToString(bytes);
  }

}
