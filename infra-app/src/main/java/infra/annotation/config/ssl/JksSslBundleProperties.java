/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.annotation.config.ssl;

import org.jspecify.annotations.Nullable;

import infra.core.ssl.jks.JksSslStoreBundle;

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
    @Nullable
    private String type;

    /**
     * Provider for the store.
     */
    @Nullable
    private String provider;

    /**
     * Location of the resource containing the store content.
     */
    @Nullable
    private String location;

    /**
     * Password used to access the store.
     */
    @Nullable
    private String password;

    @Nullable
    public String getType() {
      return this.type;
    }

    public void setType(String type) {
      this.type = type;
    }

    @Nullable
    public String getProvider() {
      return this.provider;
    }

    public void setProvider(String provider) {
      this.provider = provider;
    }

    @Nullable
    public String getLocation() {
      return this.location;
    }

    public void setLocation(String location) {
      this.location = location;
    }

    @Nullable
    public String getPassword() {
      return this.password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

  }

}
