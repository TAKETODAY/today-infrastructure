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

package infra.core.ssl.jks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyStore;
import java.util.Base64;
import java.util.function.Consumer;

import infra.app.io.ApplicationResourceLoader;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.Resource;
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
    DefaultResourceLoader loader = new ApplicationResourceLoader();
    Resource resource = loader.getResource(location);
    byte[] bytes = Files.readAllBytes(resource.getFile().toPath());
    return "base64:" + Base64.getEncoder().encodeToString(bytes);
  }

}
