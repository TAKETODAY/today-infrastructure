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

package infra.core.ssl.jks;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import infra.core.io.DefaultResourceLoader;
import infra.core.io.ResourceLoader;
import infra.core.ssl.SslStoreBundle;
import infra.core.style.ToStringBuilder;
import infra.lang.Assert;
import infra.util.StringUtils;
import infra.util.function.SingletonSupplier;

/**
 * {@link SslStoreBundle} backed by a Java keystore.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JksSslStoreBundle implements SslStoreBundle {

  @Nullable
  private final JksSslStoreDetails keyStoreDetails;

  private final ResourceLoader resourceLoader;

  private final SingletonSupplier<@Nullable KeyStore> keyStore;

  private final SingletonSupplier<@Nullable KeyStore> trustStore;

  /**
   * Create a new {@link JksSslStoreBundle} instance.
   *
   * @param keyStoreDetails the key store details
   * @param trustStoreDetails the trust store details
   */
  public JksSslStoreBundle(@Nullable JksSslStoreDetails keyStoreDetails, @Nullable JksSslStoreDetails trustStoreDetails) {
    this(keyStoreDetails, trustStoreDetails, new DefaultResourceLoader());
  }

  /**
   * Create a new {@link JksSslStoreBundle} instance.
   *
   * @param keyStoreDetails the key store details
   * @param trustStoreDetails the trust store details
   * @param resourceLoader the resource loader used to load content
   * @since 5.0
   */
  public JksSslStoreBundle(@Nullable JksSslStoreDetails keyStoreDetails, @Nullable JksSslStoreDetails trustStoreDetails, ResourceLoader resourceLoader) {
    Assert.notNull(resourceLoader, "ResourceLoader is required");
    this.keyStoreDetails = keyStoreDetails;
    this.resourceLoader = resourceLoader;
    this.keyStore = SingletonSupplier.of(() -> createKeyStore("key", keyStoreDetails));
    this.trustStore = SingletonSupplier.of(() -> createKeyStore("trust", trustStoreDetails));
  }

  @Nullable
  @Override
  public KeyStore getKeyStore() {
    return this.keyStore.get();
  }

  @Nullable
  @Override
  public String getKeyStorePassword() {
    return (this.keyStoreDetails != null) ? this.keyStoreDetails.password() : null;
  }

  @Override
  @Nullable
  public KeyStore getTrustStore() {
    return this.trustStore.get();
  }

  @Nullable
  private KeyStore createKeyStore(String name, @Nullable JksSslStoreDetails details) {
    if (details == null || details.isEmpty()) {
      return null;
    }
    try {
      String type = StringUtils.isBlank(details.type()) ? KeyStore.getDefaultType() : details.type();
      char[] password = (details.password() != null) ? details.password().toCharArray() : null;
      String location = details.location();
      KeyStore store = getKeyStoreInstance(type, details.provider());
      if (isHardwareKeystoreType(type)) {
        loadHardwareKeyStore(store, location, password);
      }
      else {
        loadKeyStore(store, location, password);
      }
      return store;
    }
    catch (Exception ex) {
      throw new IllegalStateException("Unable to create %s store: %s".formatted(name, ex.getMessage()), ex);
    }
  }

  private KeyStore getKeyStoreInstance(String type, @Nullable String provider)
          throws KeyStoreException, NoSuchProviderException {
    return (StringUtils.isBlank(provider)) ? KeyStore.getInstance(type) : KeyStore.getInstance(type, provider);
  }

  private boolean isHardwareKeystoreType(String type) {
    return type.equalsIgnoreCase("PKCS11");
  }

  private void loadHardwareKeyStore(KeyStore store, @Nullable String location, char @Nullable [] password)
          throws IOException, NoSuchAlgorithmException, CertificateException {
    if (StringUtils.hasText(location)) {
      throw new IllegalStateException(
              "Location is '%s', but must be empty or null for PKCS11 hardware key stores".formatted(location));
    }
    store.load(null, password);
  }

  private void loadKeyStore(KeyStore store, @Nullable String location, char @Nullable [] password) {
    Assert.state(StringUtils.hasText(location), "Location must not be empty or null");
    try {
      try (InputStream stream = this.resourceLoader.getResource(location).getInputStream()) {
        store.load(stream, password);
      }
    }
    catch (Exception ex) {
      throw new IllegalStateException("Could not load store from '" + location + "'", ex);
    }
  }

  @Override
  public String toString() {
    ToStringBuilder creator = new ToStringBuilder(this);
    KeyStore keyStore = this.keyStore.get();
    creator.append("keyStore.type", (keyStore != null) ? keyStore.getType() : "none");
    String keyStorePassword = getKeyStorePassword();
    creator.append("keyStorePassword", (keyStorePassword != null) ? "******" : null);
    KeyStore trustStore = this.trustStore.get();
    creator.append("trustStore.type", (trustStore != null) ? trustStore.getType() : "none");
    return creator.toString();
  }

}
