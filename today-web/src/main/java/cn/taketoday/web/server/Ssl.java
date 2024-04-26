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

package cn.taketoday.web.server;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

  /**
   * Whether to enable SSL support.
   */
  public boolean enabled = true;

  /**
   * The name of the SSL bundle to use.
   */
  @Nullable
  public String bundle;

  /**
   * The alias that identifies the key in the key store.
   */
  @Nullable
  public String keyAlias;

  /**
   * Path to the key store that holds the SSL certificates
   * (typically a jks file).
   */
  @Nullable
  public String keyStore;

  /**
   * The password used to access the key store.
   */
  @Nullable
  public String keyStorePassword;

  /**
   * The type of the key store.
   */
  @Nullable
  public String keyStoreType;

  /**
   * The provider for the key store.
   */
  @Nullable
  public String keyStoreProvider;

  /**
   * The trust store that holds SSL certificates.
   */
  @Nullable
  public String trustStore;

  /**
   * The password used to access the trust store.
   */
  @Nullable
  public String trustStorePassword;

  /**
   * The type of the trust store.
   */
  @Nullable
  public String trustStoreType;

  /**
   * The provider for the trust store.
   */
  @Nullable
  public String trustStoreProvider;

  /**
   * The location of the certificates in PEM format.
   */
  @Nullable
  public String certificate;

  /**
   * The location of the private key for the certificates in PEM format.
   */
  @Nullable
  public String certificatePrivateKey;

  /**
   * The location of the trust certificates authority chain in PEM format.
   */
  @Nullable
  public String trustCertificate;

  /**
   * The location of the private key for the trust certificates in PEM format.
   */
  @Nullable
  public String trustCertificatePrivateKey;

  /**
   * The SSL protocol to use.
   */
  public String protocol = "TLS";

  /**
   * Password used to decrypt an encrypted private key.
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
   * The handshake timeout
   */
  public Duration handshakeTimeout = Duration.ofSeconds(10);

  /**
   * The mapping of host names to SSL bundles for SNI configuration.
   */
  public final List<ServerNameSslBundle> serverNameBundles = new ArrayList<>();

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
    ssl.bundle = bundle;
    return ssl;
  }

  /**
   * @since 5.0
   */
  public static class ServerNameSslBundle {

    private String serverName;

    private String bundle;

    public void setBundle(String bundle) {
      this.bundle = bundle;
    }

    public void setServerName(String serverName) {
      this.serverName = serverName;
    }

    public String getBundle() {
      return bundle;
    }

    public String getServerName() {
      return serverName;
    }
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
