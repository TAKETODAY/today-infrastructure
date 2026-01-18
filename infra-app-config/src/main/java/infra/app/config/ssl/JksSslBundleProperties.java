/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.config.ssl;

import org.jspecify.annotations.Nullable;

import infra.app.ssl.jks.JksSslStoreBundle;

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
