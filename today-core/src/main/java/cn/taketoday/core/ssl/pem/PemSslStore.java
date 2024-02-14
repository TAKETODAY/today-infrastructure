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
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * An individual trust or key store that has been loaded from PEM content.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PemSslStoreDetails
 * @see PemContent
 * @since 4.0
 */
public interface PemSslStore {

  /**
   * The key store type, for example {@code JKS} or {@code PKCS11}. A {@code null} value
   * will use {@link KeyStore#getDefaultType()}).
   *
   * @return the key store type
   */
  @Nullable
  String type();

  /**
   * The alias used when setting entries in the {@link KeyStore}.
   *
   * @return the alias
   */
  @Nullable
  String alias();

  /**
   * The password used when
   * {@link KeyStore#setKeyEntry(String, java.security.Key, char[], java.security.cert.Certificate[])
   * setting key entries} in the {@link KeyStore}.
   *
   * @return the password
   */
  @Nullable
  String password();

  /**
   * The certificates for this store. When a {@link #privateKey() private key} is
   * present the returned value is treated as a certificate chain, otherwise it is
   * treated a list of certificates that should all be registered.
   *
   * @return the X509 certificates
   */
  List<X509Certificate> certificates();

  /**
   * The private key for this store or {@code null}.
   *
   * @return the private key
   */
  @Nullable
  PrivateKey privateKey();

  /**
   * Return a new {@link PemSslStore} instance with a new alias.
   *
   * @param alias the new alias
   * @return a new {@link PemSslStore} instance
   */
  default PemSslStore withAlias(String alias) {
    return of(type(), alias, password(), certificates(), privateKey());
  }

  /**
   * Return a new {@link PemSslStore} instance with a new password.
   *
   * @param password the new password
   * @return a new {@link PemSslStore} instance
   */
  default PemSslStore withPassword(@Nullable String password) {
    return of(type(), alias(), password, certificates(), privateKey());
  }

  /**
   * Return a {@link PemSslStore} instance loaded using the given
   * {@link PemSslStoreDetails}.
   *
   * @param details the PEM store details
   * @return a loaded {@link PemSslStore} or {@code null}.
   */
  @Nullable
  static PemSslStore load(@Nullable PemSslStoreDetails details) {
    if (details == null || details.isEmpty()) {
      return null;
    }
    return new LoadedPemSslStore(details);
  }

  /**
   * Factory method that can be used to create a new {@link PemSslStore} with the given
   * values.
   *
   * @param type the key store type
   * @param certificates the certificates for this store
   * @param privateKey the private key
   * @return a new {@link PemSslStore} instance
   */
  static PemSslStore of(String type, List<X509Certificate> certificates, PrivateKey privateKey) {
    return of(type, null, null, certificates, privateKey);
  }

  /**
   * Factory method that can be used to create a new {@link PemSslStore} with the given
   * values.
   *
   * @param certificates the certificates for this store
   * @param privateKey the private key
   * @return a new {@link PemSslStore} instance
   */
  static PemSslStore of(List<X509Certificate> certificates, PrivateKey privateKey) {
    return of(null, null, null, certificates, privateKey);
  }

  /**
   * Factory method that can be used to create a new {@link PemSslStore} with the given
   * values.
   *
   * @param type the key store type
   * @param alias the alias used when setting entries in the {@link KeyStore}
   * @param password the password used
   * {@link KeyStore#setKeyEntry(String, java.security.Key, char[], java.security.cert.Certificate[])
   * setting key entries} in the {@link KeyStore}
   * @param certificates the certificates for this store
   * @param privateKey the private key
   * @return a new {@link PemSslStore} instance
   */
  static PemSslStore of(@Nullable String type, @Nullable String alias, @Nullable String password,
          List<X509Certificate> certificates, @Nullable PrivateKey privateKey) {
    Assert.notEmpty(certificates, "Certificates must not be empty");
    return new PemSslStore() {

      @Override
      public String type() {
        return type;
      }

      @Override
      public String alias() {
        return alias;
      }

      @Override
      public String password() {
        return password;
      }

      @Override
      public List<X509Certificate> certificates() {
        return certificates;
      }

      @Override
      public PrivateKey privateKey() {
        return privateKey;
      }

    };
  }

}
