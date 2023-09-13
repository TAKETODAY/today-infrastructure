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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import cn.taketoday.core.ssl.SslStoreBundle;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
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
    this.keyStore = createKeyStore("key", keyStoreDetails, keyAlias, keyPassword);
    this.trustStore = createKeyStore("trust", trustStoreDetails, keyAlias, keyPassword);
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
  private KeyStore createKeyStore(String name, @Nullable PemSslStoreDetails details,
          @Nullable String alias, @Nullable String keyPassword) {
    if (details == null || details.isEmpty()) {
      return null;
    }
    try {
      Assert.notNull(details.certificate(), "Certificate content must not be null");
      String type = (!StringUtils.hasText(details.type())) ? KeyStore.getDefaultType() : details.type();
      KeyStore store = KeyStore.getInstance(type);
      store.load(null);
      String certificateContent = PemContent.load(details.certificate());
      String privateKeyContent = PemContent.load(details.privateKey());
      X509Certificate[] certificates = PemCertificateParser.parse(certificateContent);
      PrivateKey privateKey = PemPrivateKeyParser.parse(privateKeyContent, details.privateKeyPassword());
      addCertificates(store, certificates, privateKey, (alias != null) ? alias : DEFAULT_KEY_ALIAS, keyPassword);
      return store;
    }
    catch (Exception ex) {
      throw new IllegalStateException("Unable to create %s store: %s".formatted(name, ex.getMessage()), ex);
    }
  }

  private void addCertificates(KeyStore keyStore, @Nullable X509Certificate[] certificates,
          @Nullable PrivateKey privateKey, String alias, @Nullable String keyPassword) throws KeyStoreException {
    if (privateKey != null) {
      keyStore.setKeyEntry(alias, privateKey, (keyPassword != null) ? keyPassword.toCharArray() : null,
              certificates);
    }
    else if (certificates != null) {
      for (int index = 0; index < certificates.length; index++) {
        keyStore.setCertificateEntry(alias + "-" + index, certificates[index]);
      }
    }
  }

}
