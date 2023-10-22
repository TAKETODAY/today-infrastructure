/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.ssl.pem;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import cn.taketoday.core.ssl.SslStoreBundle;
import cn.taketoday.core.ssl.pem.KeyVerifier.Result;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link SslStoreBundle} backed by PEM-encoded certificates and private keys.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PemSslStoreBundle implements SslStoreBundle {

  private static final String DEFAULT_KEY_ALIAS = "ssl";

  @Nullable
  private final KeyStore keyStore;

  @Nullable
  private final KeyStore trustStore;

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
   * @param keyAlias the key alias to use or {@code null} to use a default alias
   */
  public PemSslStoreBundle(@Nullable PemSslStoreDetails keyStoreDetails,
          @Nullable PemSslStoreDetails trustStoreDetails, @Nullable String keyAlias) {
    this(keyStoreDetails, trustStoreDetails, keyAlias, null);
  }

  /**
   * Create a new {@link PemSslStoreBundle} instance.
   *
   * @param keyStoreDetails the key store details
   * @param trustStoreDetails the trust store details
   * @param keyAlias the key alias to use or {@code null} to use a default alias
   * @param keyPassword the password to use for the key
   */
  public PemSslStoreBundle(@Nullable PemSslStoreDetails keyStoreDetails,
          @Nullable PemSslStoreDetails trustStoreDetails, @Nullable String keyAlias, @Nullable String keyPassword) {
    this(keyStoreDetails, trustStoreDetails, keyAlias, keyPassword, false);
  }

  /**
   * Create a new {@link PemSslStoreBundle} instance.
   *
   * @param keyStoreDetails the key store details
   * @param trustStoreDetails the trust store details
   * @param keyAlias the key alias to use or {@code null} to use a default alias
   * @param keyPassword the password to use for the key
   * @param verifyKeys whether to verify that the private key matches the public key
   */
  public PemSslStoreBundle(@Nullable PemSslStoreDetails keyStoreDetails, @Nullable PemSslStoreDetails trustStoreDetails,
          @Nullable String keyAlias, @Nullable String keyPassword, boolean verifyKeys) {
    this.keyStore = createKeyStore("key", keyStoreDetails, (keyAlias != null) ? keyAlias : DEFAULT_KEY_ALIAS,
            keyPassword, verifyKeys);
    this.trustStore = createKeyStore("trust", trustStoreDetails, (keyAlias != null) ? keyAlias : DEFAULT_KEY_ALIAS,
            keyPassword, verifyKeys);
  }

  @Nullable
  @Override
  public KeyStore getKeyStore() {
    return this.keyStore;
  }

  @Nullable
  @Override
  public String getKeyStorePassword() {
    return null;
  }

  @Nullable
  @Override
  public KeyStore getTrustStore() {
    return this.trustStore;
  }

  @Nullable
  private static KeyStore createKeyStore(String name, @Nullable PemSslStoreDetails details,
          @Nullable String keyAlias, @Nullable String keyPassword, boolean verifyKeys) {
    if (details == null || details.isEmpty()) {
      return null;
    }
    try {
      Assert.notNull(details.certificate(), "Certificate content must not be null");
      KeyStore store = createKeyStore(details);
      X509Certificate[] certificates = loadCertificates(details.certificate());
      PrivateKey privateKey = loadPrivateKey(details);
      if (privateKey != null) {
        if (verifyKeys) {
          verifyKeys(privateKey, certificates);
        }
        addPrivateKey(store, privateKey, keyAlias, keyPassword, certificates);
      }
      else {
        addCertificates(store, certificates, keyAlias);
      }
      return store;
    }
    catch (Exception ex) {
      throw new IllegalStateException("Unable to create %s store: %s".formatted(name, ex.getMessage()), ex);
    }
  }

  private static void verifyKeys(PrivateKey privateKey, X509Certificate[] certificates) {
    KeyVerifier keyVerifier = new KeyVerifier();
    // Key should match one of the certificates
    for (X509Certificate certificate : certificates) {
      Result result = keyVerifier.matches(privateKey, certificate.getPublicKey());
      if (result == Result.YES) {
        return;
      }
    }
    throw new IllegalStateException("Private key matches none of the certificates");
  }

  @Nullable
  private static PrivateKey loadPrivateKey(PemSslStoreDetails details) {
    PemContent pemContent = PemContent.load(details.privateKey());
    if (pemContent == null) {
      return null;
    }
    List<PrivateKey> privateKeys = pemContent.getPrivateKeys(details.privateKeyPassword());
    Assert.state(!CollectionUtils.isEmpty(privateKeys), "Loaded private keys are empty");
    return privateKeys.get(0);
  }

  private static X509Certificate[] loadCertificates(String certificate) {
    PemContent pemContent = PemContent.load(certificate);
    assert pemContent != null;
    List<X509Certificate> certificates = pemContent.getCertificates();
    Assert.state(CollectionUtils.isNotEmpty(certificates), "Loaded certificates are empty");
    return certificates.toArray(X509Certificate[]::new);
  }

  private static KeyStore createKeyStore(PemSslStoreDetails details)
          throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    String type = StringUtils.hasText(details.type()) ? details.type() : KeyStore.getDefaultType();
    KeyStore store = KeyStore.getInstance(type);
    store.load(null);
    return store;
  }

  private static void addPrivateKey(KeyStore keyStore, PrivateKey privateKey,
          @Nullable String alias, @Nullable String keyPassword, X509Certificate[] certificates) throws KeyStoreException {
    keyStore.setKeyEntry(alias, privateKey, (keyPassword != null) ? keyPassword.toCharArray() : null, certificates);
  }

  private static void addCertificates(KeyStore keyStore, X509Certificate[] certificates, @Nullable String alias)
          throws KeyStoreException {
    for (int index = 0; index < certificates.length; index++) {
      keyStore.setCertificateEntry(alias + "-" + index, certificates[index]);
    }
  }

}
