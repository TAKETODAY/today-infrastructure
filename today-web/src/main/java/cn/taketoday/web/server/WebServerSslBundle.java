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

import java.security.KeyStore;

import cn.taketoday.core.ssl.NoSuchSslBundleException;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleKey;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.core.ssl.SslManagerBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.core.ssl.SslStoreBundle;
import cn.taketoday.core.ssl.jks.JksSslStoreBundle;
import cn.taketoday.core.ssl.jks.JksSslStoreDetails;
import cn.taketoday.core.ssl.pem.PemSslStoreBundle;
import cn.taketoday.core.ssl.pem.PemSslStoreDetails;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link SslBundle} backed by {@link Ssl}
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class WebServerSslBundle implements SslBundle {

  private final SslStoreBundle stores;

  private final SslBundleKey key;

  private final SslOptions options;

  private final String protocol;

  private final SslManagerBundle managers;

  private WebServerSslBundle(SslStoreBundle stores, @Nullable String keyPassword, Ssl ssl) {
    this.stores = stores;
    this.protocol = ssl.protocol;
    this.key = SslBundleKey.of(keyPassword, ssl.keyAlias);
    this.options = SslOptions.of(ssl.ciphers, ssl.enabledProtocols);
    this.managers = SslManagerBundle.from(this.stores, this.key);
  }

  private static SslStoreBundle createPemKeyStoreBundle(Ssl ssl) {
    PemSslStoreDetails keyStoreDetails = new PemSslStoreDetails(ssl.keyStoreType, ssl.certificate, ssl.certificatePrivateKey)
            .withAlias(ssl.keyAlias);
    return new PemSslStoreBundle(keyStoreDetails, null);
  }

  private static SslStoreBundle createPemTrustStoreBundle(Ssl ssl) {
    PemSslStoreDetails trustStoreDetails = new PemSslStoreDetails(ssl.trustStoreType,
            ssl.trustCertificate, ssl.trustCertificatePrivateKey)
            .withAlias(ssl.keyAlias);
    return new PemSslStoreBundle(null, trustStoreDetails);
  }

  private static SslStoreBundle createJksKeyStoreBundle(Ssl ssl) {
    JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(ssl.keyStoreType,
            ssl.keyStoreProvider, ssl.keyStore, ssl.keyStorePassword);
    return new JksSslStoreBundle(keyStoreDetails, null);
  }

  private static SslStoreBundle createJksTrustStoreBundle(Ssl ssl) {
    JksSslStoreDetails trustStoreDetails = new JksSslStoreDetails(ssl.trustStoreType,
            ssl.trustStoreProvider, ssl.trustStore, ssl.trustStorePassword);
    return new JksSslStoreBundle(null, trustStoreDetails);
  }

  @Override
  public SslStoreBundle getStores() {
    return this.stores;
  }

  @Override
  public SslBundleKey getKey() {
    return this.key;
  }

  @Override
  public SslOptions getOptions() {
    return this.options;
  }

  @Override
  public String getProtocol() {
    return this.protocol;
  }

  @Override
  public SslManagerBundle getManagers() {
    return this.managers;
  }

  /**
   * Get the {@link SslBundle} that should be used for the given {@link Ssl} instance.
   *
   * @param ssl the source ssl instance
   * @return a {@link SslBundle} instance
   * @throws NoSuchSslBundleException if a bundle lookup fails
   */
  public static SslBundle get(Ssl ssl) throws NoSuchSslBundleException {
    return get(ssl, null);
  }

  /**
   * Get the {@link SslBundle} that should be used for the given {@link Ssl} instance.
   *
   * @param ssl the source ssl instance
   * @param sslBundles the bundles that should be used when {@link Ssl#bundle} is
   * set
   * @return a {@link SslBundle} instance
   * @throws NoSuchSslBundleException if a bundle lookup fails
   */
  public static SslBundle get(@Nullable Ssl ssl, @Nullable SslBundles sslBundles) throws NoSuchSslBundleException {
    Assert.state(Ssl.isEnabled(ssl), "SSL is not enabled");
    String bundleName = ssl.bundle;
    if (StringUtils.hasText(bundleName)) {
      if (sslBundles == null) {
        throw new IllegalStateException(
                "SSL bundle '%s' was requested but no SslBundles instance was provided".formatted(bundleName));
      }
      return sslBundles.getBundle(bundleName);
    }
    SslStoreBundle stores = createStoreBundle(ssl);
    String keyPassword = ssl.keyPassword;
    return new WebServerSslBundle(stores, keyPassword, ssl);
  }

  private static SslStoreBundle createStoreBundle(Ssl ssl) {
    KeyStore keyStore = createKeyStore(ssl);
    KeyStore trustStore = createTrustStore(ssl);
    return new WebServerSslStoreBundle(keyStore, trustStore, ssl.keyStorePassword);
  }

  @Nullable
  private static KeyStore createKeyStore(Ssl ssl) {
    if (hasPemKeyStoreProperties(ssl)) {
      return createPemKeyStoreBundle(ssl).getKeyStore();
    }
    else if (hasJksKeyStoreProperties(ssl)) {
      return createJksKeyStoreBundle(ssl).getKeyStore();
    }
    return null;
  }

  @Nullable
  private static KeyStore createTrustStore(Ssl ssl) {
    if (hasPemTrustStoreProperties(ssl)) {
      return createPemTrustStoreBundle(ssl).getTrustStore();
    }
    else if (hasJksTrustStoreProperties(ssl)) {
      return createJksTrustStoreBundle(ssl).getTrustStore();
    }
    return null;
  }

  private static boolean hasPemKeyStoreProperties(Ssl ssl) {
    return Ssl.isEnabled(ssl) && ssl.certificate != null && ssl.certificatePrivateKey != null;
  }

  private static boolean hasPemTrustStoreProperties(Ssl ssl) {
    return Ssl.isEnabled(ssl) && ssl.trustCertificate != null;
  }

  private static boolean hasJksKeyStoreProperties(Ssl ssl) {
    return Ssl.isEnabled(ssl)
            && (ssl.keyStore != null || (ssl.keyStoreType != null && ssl.keyStoreType.equals("PKCS11")));
  }

  private static boolean hasJksTrustStoreProperties(Ssl ssl) {
    return Ssl.isEnabled(ssl) && (ssl.trustStore != null
            || (ssl.trustStoreType != null && ssl.trustStoreType.equals("PKCS11")));
  }

  @Override
  public String toString() {
    ToStringBuilder creator = new ToStringBuilder(this);
    creator.append("key", this.key);
    creator.append("protocol", this.protocol);
    creator.append("stores", this.stores);
    creator.append("options", this.options);
    return creator.toString();
  }

  private static final class WebServerSslStoreBundle implements SslStoreBundle {

    @Nullable
    private final KeyStore keyStore;

    @Nullable
    private final KeyStore trustStore;

    @Nullable
    private final String keyStorePassword;

    private WebServerSslStoreBundle(@Nullable KeyStore keyStore, @Nullable KeyStore trustStore, @Nullable String keyStorePassword) {
      Assert.state(keyStore != null || trustStore != null, "SSL is enabled but no trust material is configured");
      this.keyStore = keyStore;
      this.trustStore = trustStore;
      this.keyStorePassword = keyStorePassword;
    }

    @Nullable
    @Override
    public KeyStore getKeyStore() {
      return this.keyStore;
    }

    @Nullable
    @Override
    public KeyStore getTrustStore() {
      return this.trustStore;
    }

    @Nullable
    @Override
    public String getKeyStorePassword() {
      return this.keyStorePassword;
    }

    @Override
    public String toString() {
      ToStringBuilder creator = new ToStringBuilder(this);
      creator.append("keyStore.type", (this.keyStore != null) ? this.keyStore.getType() : "none");
      creator.append("keyStorePassword", (this.keyStorePassword != null) ? "******" : null);
      creator.append("trustStore.type", (this.trustStore != null) ? this.trustStore.getType() : "none");
      return creator.toString();
    }

  }

}
