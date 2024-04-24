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

package cn.taketoday.framework.web.server;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;

/**
 * Simple server-independent abstraction for SSL configuration.
 *
 * @author Andy Wilkinson
 * @author Vladimir Tsanev
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Ssl {

  @Nullable
  private String bundle;

  @Nullable
  private String keyAlias;

  @Nullable
  private String keyStore;

  @Nullable
  private String keyStorePassword;

  @Nullable
  private String keyStoreType;

  @Nullable
  private String keyStoreProvider;

  @Nullable
  private String trustStore;

  @Nullable
  private String trustStorePassword;

  @Nullable
  private String trustStoreType;

  @Nullable
  private String trustStoreProvider;

  @Nullable
  private String certificate;

  @Nullable
  private String certificatePrivateKey;

  @Nullable
  private String trustCertificate;

  @Nullable
  private String trustCertificatePrivateKey;

  private String protocol = "TLS";

  /**
   * Whether to enable SSL support.
   */
  public boolean enabled = false;

  /**
   * Public key resource location
   */
  public Resource publicKey;

  /**
   * Private key resource location
   */
  public Resource privateKey;

  /**
   * Private key password
   */
  @Nullable
  public String keyPassword;

  /**
   * The supported SSL ciphers
   */
  @Nullable
  public String[] ciphers;

  /**
   * The enabled SSL protocols.
   */
  @Nullable
  public String[] enabledProtocols;

  /**
   * Return Whether client authentication is not wanted ("none"), wanted ("want") or
   * needed ("need"). Requires a trust store.
   */
  @Nullable
  public ClientAuth clientAuth;

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
   * Return the name of the SSL bundle to use.
   *
   * @return the SSL bundle name
   */
  @Nullable
  public String getBundle() {
    return this.bundle;
  }

  /**
   * Set the name of the SSL bundle to use.
   *
   * @param bundle the SSL bundle name
   */
  public void setBundle(@Nullable String bundle) {
    this.bundle = bundle;
  }

  /**
   * Return Whether client authentication is not wanted ("none"), wanted ("want") or
   * needed ("need"). Requires a trust store.
   *
   * @return the {@link ClientAuth} to use
   */
  @Nullable
  public ClientAuth getClientAuth() {
    return this.clientAuth;
  }

  public void setClientAuth(@Nullable ClientAuth clientAuth) {
    this.clientAuth = clientAuth;
  }

  /**
   * Return the supported SSL ciphers.
   *
   * @return the supported SSL ciphers
   */
  @Nullable
  public String[] getCiphers() {
    return this.ciphers;
  }

  public void setCiphers(@Nullable String[] ciphers) {
    this.ciphers = ciphers;
  }

  /**
   * Return the enabled SSL protocols.
   *
   * @return the enabled SSL protocols.
   */
  @Nullable
  public String[] getEnabledProtocols() {
    return this.enabledProtocols;
  }

  public void setEnabledProtocols(@Nullable String[] enabledProtocols) {
    this.enabledProtocols = enabledProtocols;
  }

  /**
   * Return the alias that identifies the key in the key store.
   *
   * @return the key alias
   */
  @Nullable
  public String getKeyAlias() {
    return this.keyAlias;
  }

  public void setKeyAlias(@Nullable String keyAlias) {
    this.keyAlias = keyAlias;
  }

  /**
   * Return the password used to access the key in the key store.
   *
   * @return the key password
   */
  @Nullable
  public String getKeyPassword() {
    return this.keyPassword;
  }

  public void setKeyPassword(@Nullable String keyPassword) {
    this.keyPassword = keyPassword;
  }

  /**
   * Return the path to the key store that holds the SSL certificates (typically a jks
   * file).
   *
   * @return the path to the key store
   */
  @Nullable
  public String getKeyStore() {
    return this.keyStore;
  }

  public void setKeyStore(@Nullable String keyStore) {
    this.keyStore = keyStore;
  }

  /**
   * Return the password used to access the key store.
   *
   * @return the key store password
   */
  @Nullable
  public String getKeyStorePassword() {
    return this.keyStorePassword;
  }

  public void setKeyStorePassword(@Nullable String keyStorePassword) {
    this.keyStorePassword = keyStorePassword;
  }

  /**
   * Return the type of the key store.
   *
   * @return the key store type
   */
  @Nullable
  public String getKeyStoreType() {
    return this.keyStoreType;
  }

  public void setKeyStoreType(@Nullable String keyStoreType) {
    this.keyStoreType = keyStoreType;
  }

  /**
   * Return the provider for the key store.
   *
   * @return the key store provider
   */
  @Nullable
  public String getKeyStoreProvider() {
    return this.keyStoreProvider;
  }

  public void setKeyStoreProvider(@Nullable String keyStoreProvider) {
    this.keyStoreProvider = keyStoreProvider;
  }

  /**
   * Return the trust store that holds SSL certificates.
   *
   * @return the trust store
   */
  @Nullable
  public String getTrustStore() {
    return this.trustStore;
  }

  public void setTrustStore(@Nullable String trustStore) {
    this.trustStore = trustStore;
  }

  /**
   * Return the password used to access the trust store.
   *
   * @return the trust store password
   */
  @Nullable
  public String getTrustStorePassword() {
    return this.trustStorePassword;
  }

  public void setTrustStorePassword(@Nullable String trustStorePassword) {
    this.trustStorePassword = trustStorePassword;
  }

  /**
   * Return the type of the trust store.
   *
   * @return the trust store type
   */
  @Nullable
  public String getTrustStoreType() {
    return this.trustStoreType;
  }

  public void setTrustStoreType(@Nullable String trustStoreType) {
    this.trustStoreType = trustStoreType;
  }

  /**
   * Return the provider for the trust store.
   *
   * @return the trust store provider
   */
  @Nullable
  public String getTrustStoreProvider() {
    return this.trustStoreProvider;
  }

  public void setTrustStoreProvider(@Nullable String trustStoreProvider) {
    this.trustStoreProvider = trustStoreProvider;
  }

  /**
   * Return the location of the certificates in PEM format.
   *
   * @return the certificates location
   */
  @Nullable
  public String getCertificate() {
    return this.certificate;
  }

  public void setCertificate(@Nullable String certificate) {
    this.certificate = certificate;
  }

  /**
   * Return the location of the private key for the certificates in PEM format.
   *
   * @return the location of the certificates private key
   */
  @Nullable
  public String getCertificatePrivateKey() {
    return this.certificatePrivateKey;
  }

  public void setCertificatePrivateKey(@Nullable String certificatePrivateKey) {
    this.certificatePrivateKey = certificatePrivateKey;
  }

  /**
   * Return the location of the trust certificates authority chain in PEM format.
   *
   * @return the location of the trust certificates
   */
  @Nullable
  public String getTrustCertificate() {
    return this.trustCertificate;
  }

  public void setTrustCertificate(@Nullable String trustCertificate) {
    this.trustCertificate = trustCertificate;
  }

  /**
   * Return the location of the private key for the trust certificates in PEM format.
   *
   * @return the location of the trust certificates private key
   */
  @Nullable
  public String getTrustCertificatePrivateKey() {
    return this.trustCertificatePrivateKey;
  }

  public void setTrustCertificatePrivateKey(@Nullable String trustCertificatePrivateKey) {
    this.trustCertificatePrivateKey = trustCertificatePrivateKey;
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
   * Returns if SSL is enabled for the given instance.
   *
   * @param ssl the {@link Ssl SSL} instance or {@code null}
   * @return {@code true} is SSL is enabled
   */
  public static boolean isEnabled(@Nullable Ssl ssl) {
    return ssl != null && ssl.enabled;
  }

  /**
   * Factory method to create an {@link Ssl} instance for a specific bundle name.
   *
   * @param bundle the name of the bundle
   * @return a new {@link Ssl} instance with the bundle set
   */
  public static Ssl forBundle(@Nullable String bundle) {
    Ssl ssl = new Ssl();
    ssl.setBundle(bundle);
    return ssl;
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
    NEED;

    /**
     * Map an optional {@link ClientAuth} value to a different type.
     *
     * @param <R> the result type
     * @param clientAuth the client auth to map (may be {@code null})
     * @param none the value for {@link ClientAuth#NONE} or {@code null}
     * @param want the value for {@link ClientAuth#WANT}
     * @param need the value for {@link ClientAuth#NEED}
     * @return the mapped value
     */
    public static <R> R map(@Nullable ClientAuth clientAuth, R none, R want, R need) {
      return switch (clientAuth != null ? clientAuth : NONE) {
        case NONE -> none;
        case WANT -> want;
        case NEED -> need;
      };
    }

  }

}
