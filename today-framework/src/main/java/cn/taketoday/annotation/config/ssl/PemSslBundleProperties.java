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

package cn.taketoday.annotation.config.ssl;

import cn.taketoday.core.ssl.pem.PemSslStoreBundle;

/**
 * {@link SslBundleProperties} for PEM-encoded certificates and private keys.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @see PemSslStoreBundle
 * @since 4.0
 */
public class PemSslBundleProperties extends SslBundleProperties {

  /**
   * Keystore properties.
   */
  private final Store keystore = new Store();

  /**
   * Truststore properties.
   */
  private final Store truststore = new Store();

  public Store getKeystore() {
    return this.keystore;
  }

  public Store getTruststore() {
    return this.truststore;
  }

  /**
   * Store properties.
   */
  public static class Store {

    /**
     * Type of the store to create, e.g. JKS.
     */
    String type;

    /**
     * Location or content of the certificate in PEM format.
     */
    String certificate;

    /**
     * Location or content of the private key in PEM format.
     */
    String privateKey;

    public String getType() {
      return this.type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getCertificate() {
      return this.certificate;
    }

    public void setCertificate(String certificate) {
      this.certificate = certificate;
    }

    public String getPrivateKey() {
      return this.privateKey;
    }

    public void setPrivateKey(String privateKey) {
      this.privateKey = privateKey;
    }

  }

}
