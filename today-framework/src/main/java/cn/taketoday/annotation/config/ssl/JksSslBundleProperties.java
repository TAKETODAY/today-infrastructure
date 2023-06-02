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

import cn.taketoday.core.ssl.jks.JksSslStoreBundle;

/**
 * {@link SslBundleProperties} for Java keystores.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @see JksSslStoreBundle
 * @since 4.0
 */
public class JksSslBundleProperties extends SslBundleProperties {

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
    private String type;

    /**
     * Provider for the store.
     */
    private String provider;

    /**
     * Location of the resource containing the store content.
     */
    private String location;

    /**
     * Password used to access the store.
     */
    private String password;

    public String getType() {
      return this.type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getProvider() {
      return this.provider;
    }

    public void setProvider(String provider) {
      this.provider = provider;
    }

    public String getLocation() {
      return this.location;
    }

    public void setLocation(String location) {
      this.location = location;
    }

    public String getPassword() {
      return this.password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

  }

}
