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

import java.util.Set;

import cn.taketoday.core.ssl.SslBundle;

/**
 * Base class for SSL Bundle properties.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @see SslBundle
 * @since 4.0
 */
public abstract class SslBundleProperties {

  /**
   * Key details for the bundle.
   */
  private final Key key = new Key();

  /**
   * Options for the SLL connection.
   */
  private final Options options = new Options();

  /**
   * SSL Protocol to use.
   */
  private String protocol = SslBundle.DEFAULT_PROTOCOL;

  public Key getKey() {
    return this.key;
  }

  public Options getOptions() {
    return this.options;
  }

  public String getProtocol() {
    return this.protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public static class Options {

    /**
     * Supported SSL ciphers.
     */
    private Set<String> ciphers;

    /**
     * Enabled SSL protocols.
     */
    private Set<String> enabledProtocols;

    public Set<String> getCiphers() {
      return this.ciphers;
    }

    public void setCiphers(Set<String> ciphers) {
      this.ciphers = ciphers;
    }

    public Set<String> getEnabledProtocols() {
      return this.enabledProtocols;
    }

    public void setEnabledProtocols(Set<String> enabledProtocols) {
      this.enabledProtocols = enabledProtocols;
    }

  }

  public static class Key {

    /**
     * The password used to access the key in the key store.
     */
    private String password;

    /**
     * The alias that identifies the key in the key store.
     */
    private String alias;

    public String getPassword() {
      return this.password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getAlias() {
      return this.alias;
    }

    public void setAlias(String alias) {
      this.alias = alias;
    }

  }

}
