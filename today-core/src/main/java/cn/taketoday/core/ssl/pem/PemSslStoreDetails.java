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

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * Details for an individual trust or key store in a {@link PemSslStoreBundle}.
 *
 * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
 * {@code null} value will use {@link KeyStore#getDefaultType()}).
 * @param certificate the certificate content (either the PEM content itself or something
 * that can be loaded by {@link ResourceUtils#getURL})
 * @param privateKey the private key content (either the PEM content itself or something
 * that can be loaded by {@link ResourceUtils#getURL})
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public record PemSslStoreDetails(
        @Nullable String type, @Nullable String certificate, @Nullable String privateKey) {

  /**
   * Return a new {@link PemSslStoreDetails} instance with a new private key.
   *
   * @param privateKey the new private key
   * @return a new {@link PemSslStoreDetails} instance
   */
  public PemSslStoreDetails withPrivateKey(String privateKey) {
    return new PemSslStoreDetails(this.type, this.certificate, privateKey);
  }

  boolean isEmpty() {
    return isEmpty(this.type) && isEmpty(this.certificate) && isEmpty(this.privateKey);
  }

  private boolean isEmpty(@Nullable String value) {
    return StringUtils.isBlank(value);
  }

  /**
   * Factory method to create a new {@link PemSslStoreDetails} instance for the given
   * certificate.
   *
   * @param certificate the certificate
   * @return a new {@link PemSslStoreDetails} instance.
   */
  public static PemSslStoreDetails forCertificate(@Nullable String certificate) {
    return new PemSslStoreDetails(null, certificate, null);
  }

}
