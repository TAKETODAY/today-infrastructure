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
  private final PemSslStoreDetails keyStoreDetails;

  @Nullable
  private final PemSslStoreDetails trustStoreDetails;

  @Nullable
  private final String keyAlias;

  @Nullable
  private final String keyPassword;

  /**
   * Create a new {@link PemSslStoreBundle} instance.
   *
   * @param keyStoreDetails the key store details
   * @param trustStoreDetails the trust store details
   */
  public PemSslStoreBundle(PemSslStoreDetails keyStoreDetails, PemSslStoreDetails trustStoreDetails) {
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
          @Nullable PemSslStoreDetails trustStoreDetails, @Nullable String keyAlias,
          @Nullable String keyPassword) {
    this.keyAlias = keyAlias;
    this.keyStoreDetails = keyStoreDetails;
    this.trustStoreDetails = trustStoreDetails;
    this.keyPassword = keyPassword;
  }

  @Override
  public KeyStore getKeyStore() {
    return createKeyStore("key", this.keyStoreDetails);
  }

  @Override
  public String getKeyStorePassword() {
    return null;
  }

  @Override
  public KeyStore getTrustStore() {
    return createKeyStore("trust", this.trustStoreDetails);
  }

  @Nullable
  private KeyStore createKeyStore(String name, @Nullable PemSslStoreDetails details) {
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
      addCertificates(store, certificates, privateKey);
      return store;
    }
    catch (Exception ex) {
      throw new IllegalStateException("Unable to create %s store: %s".formatted(name, ex.getMessage()), ex);
    }
  }

  private void addCertificates(KeyStore keyStore, @Nullable X509Certificate[] certificates,
          @Nullable PrivateKey privateKey) throws KeyStoreException {
    String alias = (this.keyAlias != null) ? this.keyAlias : DEFAULT_KEY_ALIAS;
    if (privateKey != null) {
      keyStore.setKeyEntry(alias, privateKey,
              (this.keyPassword != null) ? this.keyPassword.toCharArray() : null, certificates);
    }
    else if (certificates != null) {
      for (int index = 0; index < certificates.length; index++) {
        keyStore.setCertificateEntry(alias + "-" + index, certificates[index]);
      }
    }
  }

}
