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

import java.security.KeyStore;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Details for an individual trust or key store in a {@link PemSslStoreBundle}.
 *
 * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
 * {@code null} value will use {@link KeyStore#getDefaultType()}).
 * @param alias the alias used when setting entries in the {@link KeyStore}
 * @param password the password used
 * {@link KeyStore#setKeyEntry(String, java.security.Key, char[], java.security.cert.Certificate[])
 * setting key entries} in the {@link KeyStore}
 * @param certificates the certificates content (either the PEM content itself or or a
 * reference to the resource to load). When a {@link #privateKey() private key} is present
 * this value is treated as a certificate chain, otherwise it is treated a list of
 * certificates that should all be registered.
 * @param privateKey the private key content (either the PEM content itself or a reference
 * to the resource to load)
 * @param privateKeyPassword a password used to decrypt an encrypted private key
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public record PemSslStoreDetails(@Nullable String type, @Nullable String alias, @Nullable String password,
        @Nullable String certificates, @Nullable String privateKey, @Nullable String privateKeyPassword) {

  /**
   * Create a new {@link PemSslStoreDetails} instance.
   *
   * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
   * {@code null} value will use {@link KeyStore#getDefaultType()}).
   * @param certificate the certificates content (either the PEM content itself or a
   * reference to the resource to load)
   * @param privateKey the private key content (either the PEM content itself or a
   * reference to the resource to load)
   */
  public PemSslStoreDetails(@Nullable String type, @Nullable String certificate, @Nullable String privateKey) {
    this(type, certificate, privateKey, null);
  }

  /**
   * Create a new {@link PemSslStoreDetails} instance.
   *
   * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
   * {@code null} value will use {@link KeyStore#getDefaultType()}).
   * @param certificate the certificates content (either the PEM content itself or a
   * reference to the resource to load)
   * @param privateKey the private key content (either the PEM content itself or a
   * reference to the resource to load)
   * @param privateKeyPassword a password used to decrypt an encrypted private key
   */
  public PemSslStoreDetails(@Nullable String type, @Nullable String certificate,
          @Nullable String privateKey, @Nullable String privateKeyPassword) {
    this(type, null, null, certificate, privateKey, privateKeyPassword);
  }

  /**
   * Return a new {@link PemSslStoreDetails} instance with a new private key.
   *
   * @param privateKey the new private key
   * @return a new {@link PemSslStoreDetails} instance
   */
  public PemSslStoreDetails withPrivateKey(@Nullable String privateKey) {
    return new PemSslStoreDetails(this.type, this.certificates, privateKey, this.privateKeyPassword);
  }

  /**
   * Return a new {@link PemSslStoreDetails} instance with a new private key password.
   *
   * @param password the new private key password
   * @return a new {@link PemSslStoreDetails} instance
   */
  public PemSslStoreDetails withPrivateKeyPassword(@Nullable String password) {
    return new PemSslStoreDetails(this.type, this.certificates, this.privateKey, password);
  }

  /**
   * Return a new {@link PemSslStoreDetails} instance with a new alias.
   *
   * @param alias the new alias
   * @return a new {@link PemSslStoreDetails} instance
   */
  public PemSslStoreDetails withAlias(@Nullable String alias) {
    return new PemSslStoreDetails(this.type, alias, this.password, this.certificates, this.privateKey,
            this.privateKeyPassword);
  }

  /**
   * Return a new {@link PemSslStoreDetails} instance with a new password.
   *
   * @param password the new password
   * @return a new {@link PemSslStoreDetails} instance
   */
  public PemSslStoreDetails withPassword(@Nullable String password) {
    return new PemSslStoreDetails(this.type, this.alias, password, this.certificates, this.privateKey,
            this.privateKeyPassword);
  }

  boolean isEmpty() {
    return isEmpty(this.type) && isEmpty(this.certificates) && isEmpty(this.privateKey);
  }

  private boolean isEmpty(@Nullable String value) {
    return !StringUtils.hasText(value);
  }

  /**
   * Factory method to create a new {@link PemSslStoreDetails} instance for the given
   * certificate. <b>Note:</b> This method doesn't actually check if the provided value
   * only contains a single certificate. It is functionally equivalent to
   * {@link #forCertificates(String)}.
   *
   * @param certificate the certificate content (either the PEM content itself or a
   * reference to the resource to load)
   * @return a new {@link PemSslStoreDetails} instance.
   */
  public static PemSslStoreDetails forCertificate(@Nullable String certificate) {
    return forCertificates(certificate);

  }

  /**
   * Factory method to create a new {@link PemSslStoreDetails} instance for the given
   * certificates.
   *
   * @param certificates the certificates content (either the PEM content itself or a
   * reference to the resource to load)
   * @return a new {@link PemSslStoreDetails} instance.
   */
  public static PemSslStoreDetails forCertificates(@Nullable String certificates) {
    return new PemSslStoreDetails(null, certificates, null);
  }

}
