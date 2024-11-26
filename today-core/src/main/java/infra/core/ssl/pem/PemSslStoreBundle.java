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

package infra.core.ssl.pem;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Supplier;

import infra.core.ssl.SslStoreBundle;
import infra.core.style.ToStringBuilder;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.StringUtils;
import infra.util.function.SingletonSupplier;

/**
 * {@link SslStoreBundle} backed by PEM-encoded certificates and private keys.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PemSslStoreBundle implements SslStoreBundle {

  private static final String DEFAULT_ALIAS = "ssl";

  private final Supplier<KeyStore> keyStore;

  private final Supplier<KeyStore> trustStore;

  /**
   * Create a new {@link PemSslStoreBundle} instance.
   *
   * @param keyStoreDetails the key store details
   * @param trustStoreDetails the trust store details
   */
  public PemSslStoreBundle(@Nullable PemSslStoreDetails keyStoreDetails, @Nullable PemSslStoreDetails trustStoreDetails) {
    this(keyStoreDetails, trustStoreDetails, null);
  }

  /**
   * Create a new {@link PemSslStoreBundle} instance.
   *
   * @param keyStoreDetails the key store details
   * @param trustStoreDetails the trust store details
   * @param alias the alias to use or {@code null} to use a default alias
   */
  public PemSslStoreBundle(@Nullable PemSslStoreDetails keyStoreDetails, @Nullable PemSslStoreDetails trustStoreDetails, @Nullable String alias) {
    this(PemSslStore.load(keyStoreDetails), PemSslStore.load(trustStoreDetails));
  }

  /**
   * Create a new {@link PemSslStoreBundle} instance.
   *
   * @param pemKeyStore the PEM key store
   * @param pemTrustStore the PEM trust store
   */
  public PemSslStoreBundle(@Nullable PemSslStore pemKeyStore, @Nullable PemSslStore pemTrustStore) {
    this.keyStore = SingletonSupplier.from(() -> createKeyStore("key", pemKeyStore));
    this.trustStore = SingletonSupplier.from(() -> createKeyStore("trust", pemTrustStore));
  }

  @Nullable
  @Override
  public KeyStore getKeyStore() {
    return this.keyStore.get();
  }

  @Nullable
  @Override
  public String getKeyStorePassword() {
    return null;
  }

  @Nullable
  @Override
  public KeyStore getTrustStore() {
    return this.trustStore.get();
  }

  @Nullable
  private static KeyStore createKeyStore(String name, @Nullable PemSslStore pemSslStore) {
    if (pemSslStore == null) {
      return null;
    }
    try {
      List<X509Certificate> certificates = pemSslStore.certificates();
      Assert.notEmpty(certificates, "Certificates must not be empty");
      String alias = pemSslStore.alias();
      alias = alias != null ? alias : DEFAULT_ALIAS;
      KeyStore store = createKeyStore(pemSslStore.type());
      PrivateKey privateKey = pemSslStore.privateKey();
      if (privateKey != null) {
        addPrivateKey(store, privateKey, alias, pemSslStore.password(), certificates);
      }
      else {
        addCertificates(store, certificates, alias);
      }
      return store;
    }
    catch (Exception ex) {
      throw new IllegalStateException("Unable to create %s store: %s".formatted(name, ex.getMessage()), ex);
    }
  }

  private static KeyStore createKeyStore(@Nullable String type)
          throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    KeyStore store = KeyStore.getInstance(StringUtils.hasText(type) ? type : KeyStore.getDefaultType());
    store.load(null);
    return store;
  }

  private static void addPrivateKey(KeyStore keyStore, PrivateKey privateKey, String alias, @Nullable String keyPassword,
          List<X509Certificate> certificateChain) throws KeyStoreException {
    keyStore.setKeyEntry(alias, privateKey, (keyPassword != null) ? keyPassword.toCharArray() : null,
            certificateChain.toArray(X509Certificate[]::new));
  }

  private static void addCertificates(KeyStore keyStore, List<X509Certificate> certificates, String alias)
          throws KeyStoreException {
    for (int index = 0; index < certificates.size(); index++) {
      String entryAlias = alias + ((certificates.size() == 1) ? "" : "-" + index);
      X509Certificate certificate = certificates.get(index);
      keyStore.setCertificateEntry(entryAlias, certificate);
    }
  }

  @Override
  public String toString() {
    ToStringBuilder creator = new ToStringBuilder(this);
    KeyStore keyStore = this.keyStore.get();
    KeyStore trustStore = this.trustStore.get();
    creator.append("keyStore.type", (keyStore != null) ? keyStore.getType() : "none");
    creator.append("keyStorePassword", null);
    creator.append("trustStore.type", (trustStore != null) ? trustStore.getType() : "none");
    return creator.toString();
  }

}
