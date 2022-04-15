/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.server;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * An {@link SslStoreProvider} that creates key and trust stores from certificate and
 * private key PEM files.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/15 12:32
 */
public final class CertificateFileSslStoreProvider implements SslStoreProvider {

  private static final char[] NO_PASSWORD = {};

  private static final String DEFAULT_KEY_ALIAS = "today-web";

  private final Ssl ssl;

  private CertificateFileSslStoreProvider(Ssl ssl) {
    this.ssl = ssl;
  }

  @Override
  public KeyStore getKeyStore() throws Exception {
    return createKeyStore(this.ssl.getCertificate(), this.ssl.getCertificatePrivateKey(),
            this.ssl.getKeyStorePassword(), this.ssl.getKeyStoreType(), this.ssl.getKeyAlias());
  }

  @Override
  public KeyStore getTrustStore() throws Exception {
    if (this.ssl.getTrustCertificate() == null) {
      return null;
    }
    return createKeyStore(this.ssl.getTrustCertificate(), this.ssl.getTrustCertificatePrivateKey(),
            this.ssl.getTrustStorePassword(), this.ssl.getTrustStoreType(), this.ssl.getKeyAlias());
  }

  /**
   * Create a new {@link KeyStore} populated with the certificate stored at the
   * specified file path and an optional private key.
   *
   * @param certPath the path to the certificate authority file
   * @param keyPath the path to the private file
   * @param password the key store password
   * @param storeType the {@code KeyStore} type to create
   * @param keyAlias the alias to use when adding keys to the {@code KeyStore}
   * @return the {@code KeyStore}
   */
  private KeyStore createKeyStore(String certPath,
          String keyPath, String password, String storeType, String keyAlias) {
    try {
      KeyStore keyStore = KeyStore.getInstance((storeType != null) ? storeType : KeyStore.getDefaultType());
      keyStore.load(null);
      X509Certificate[] certificates = CertificateParser.parse(certPath);
      PrivateKey privateKey = (keyPath != null) ? PrivateKeyParser.parse(keyPath) : null;
      try {
        addCertificates(keyStore, certificates, privateKey, password, keyAlias);
      }
      catch (KeyStoreException ex) {
        throw new IllegalStateException("Error adding certificates to KeyStore: " + ex.getMessage(), ex);
      }
      return keyStore;
    }
    catch (GeneralSecurityException | IOException ex) {
      throw new IllegalStateException("Error creating KeyStore: " + ex.getMessage(), ex);
    }
  }

  private void addCertificates(KeyStore keyStore, X509Certificate[] certificates,
          PrivateKey privateKey, String password, String keyAlias) throws KeyStoreException {
    String alias = (keyAlias != null) ? keyAlias : DEFAULT_KEY_ALIAS;
    if (privateKey != null) {
      keyStore.setKeyEntry(alias, privateKey, ((password != null) ? password.toCharArray() : NO_PASSWORD),
              certificates);
    }
    else {
      for (int index = 0; index < certificates.length; index++) {
        keyStore.setCertificateEntry(alias + "-" + index, certificates[index]);
      }
    }
  }

  /**
   * Create a {@link SslStoreProvider} if the appropriate SSL properties are configured.
   *
   * @param ssl the SSL properties
   * @return a {@code SslStoreProvider} or {@code null}
   */
  public static SslStoreProvider from(Ssl ssl) {
    if (ssl != null && ssl.isEnabled()) {
      if (ssl.getCertificate() != null && ssl.getCertificatePrivateKey() != null) {
        return new CertificateFileSslStoreProvider(ssl);
      }
    }
    return null;
  }

}
