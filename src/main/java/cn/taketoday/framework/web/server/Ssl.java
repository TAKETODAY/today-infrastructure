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

/**
 * Simple server-independent abstraction for SSL configuration.
 *
 * @author Andy Wilkinson
 * @author Vladimir Tsanev
 * @author Stephane Nicoll
 * @since 4.0
 */
public class Ssl {

  private boolean enabled = true;

  private ClientAuth clientAuth;

  private String[] ciphers;

  private String[] enabledProtocols;

  private String keyAlias;

  private String keyPassword;

  private String keyStore;

  private String keyStorePassword;

  private String keyStoreType;

  private String keyStoreProvider;

  private String trustStore;

  private String trustStorePassword;

  private String trustStoreType;

  private String trustStoreProvider;

  private String protocol = "TLS";

  /**
   * Return whether to enable SSL support.
   *
   * @return whether to enable SSL support
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Return Whether client authentication is not wanted ("none"), wanted ("want") or
   * needed ("need"). Requires a trust store.
   *
   * @return the {@link ClientAuth} to use
   */
  public ClientAuth getClientAuth() {
    return this.clientAuth;
  }

  public void setClientAuth(ClientAuth clientAuth) {
    this.clientAuth = clientAuth;
  }

  /**
   * Return the supported SSL ciphers.
   *
   * @return the supported SSL ciphers
   */
  public String[] getCiphers() {
    return this.ciphers;
  }

  public void setCiphers(String[] ciphers) {
    this.ciphers = ciphers;
  }

  /**
   * Return the enabled SSL protocols.
   *
   * @return the enabled SSL protocols.
   */
  public String[] getEnabledProtocols() {
    return this.enabledProtocols;
  }

  public void setEnabledProtocols(String[] enabledProtocols) {
    this.enabledProtocols = enabledProtocols;
  }

  /**
   * Return the alias that identifies the key in the key store.
   *
   * @return the key alias
   */
  public String getKeyAlias() {
    return this.keyAlias;
  }

  public void setKeyAlias(String keyAlias) {
    this.keyAlias = keyAlias;
  }

  /**
   * Return the password used to access the key in the key store.
   *
   * @return the key password
   */
  public String getKeyPassword() {
    return this.keyPassword;
  }

  public void setKeyPassword(String keyPassword) {
    this.keyPassword = keyPassword;
  }

  /**
   * Return the path to the key store that holds the SSL certificate (typically a jks
   * file).
   *
   * @return the path to the key store
   */
  public String getKeyStore() {
    return this.keyStore;
  }

  public void setKeyStore(String keyStore) {
    this.keyStore = keyStore;
  }

  /**
   * Return the password used to access the key store.
   *
   * @return the key store password
   */
  public String getKeyStorePassword() {
    return this.keyStorePassword;
  }

  public void setKeyStorePassword(String keyStorePassword) {
    this.keyStorePassword = keyStorePassword;
  }

  /**
   * Return the type of the key store.
   *
   * @return the key store type
   */
  public String getKeyStoreType() {
    return this.keyStoreType;
  }

  public void setKeyStoreType(String keyStoreType) {
    this.keyStoreType = keyStoreType;
  }

  /**
   * Return the provider for the key store.
   *
   * @return the key store provider
   */
  public String getKeyStoreProvider() {
    return this.keyStoreProvider;
  }

  public void setKeyStoreProvider(String keyStoreProvider) {
    this.keyStoreProvider = keyStoreProvider;
  }

  /**
   * Return the trust store that holds SSL certificates.
   *
   * @return the trust store
   */
  public String getTrustStore() {
    return this.trustStore;
  }

  public void setTrustStore(String trustStore) {
    this.trustStore = trustStore;
  }

  /**
   * Return the password used to access the trust store.
   *
   * @return the trust store password
   */
  public String getTrustStorePassword() {
    return this.trustStorePassword;
  }

  public void setTrustStorePassword(String trustStorePassword) {
    this.trustStorePassword = trustStorePassword;
  }

  /**
   * Return the type of the trust store.
   *
   * @return the trust store type
   */
  public String getTrustStoreType() {
    return this.trustStoreType;
  }

  public void setTrustStoreType(String trustStoreType) {
    this.trustStoreType = trustStoreType;
  }

  /**
   * Return the provider for the trust store.
   *
   * @return the trust store provider
   */
  public String getTrustStoreProvider() {
    return this.trustStoreProvider;
  }

  public void setTrustStoreProvider(String trustStoreProvider) {
    this.trustStoreProvider = trustStoreProvider;
  }

  /**
   * Return the SSL protocol to use.
   *
   * @return the SSL protocol
   */
  public String getProtocol() {
    return this.protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  /**
   * Client authentication types.
   */
  public enum ClientAuth {

    /**
     * Client authentication is not wanted.
     */
    NONE,

    /**
     * Client authentication is wanted but not mandatory.
     */
    WANT,

    /**
     * Client authentication is needed and mandatory.
     */
    NEED

  }

}
