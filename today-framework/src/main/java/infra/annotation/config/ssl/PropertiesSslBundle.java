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

import infra.app.io.ApplicationResourceLoader;
import infra.core.io.ResourceLoader;
import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundleKey;
import infra.core.ssl.SslManagerBundle;
import infra.core.ssl.SslOptions;
import infra.core.ssl.SslStoreBundle;
import infra.core.ssl.jks.JksSslStoreBundle;
import infra.core.ssl.jks.JksSslStoreDetails;
import infra.core.ssl.pem.PemSslStore;
import infra.core.ssl.pem.PemSslStoreBundle;
import infra.core.ssl.pem.PemSslStoreDetails;
import infra.core.style.ToStringBuilder;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * {@link SslBundle} backed by {@link JksSslBundleProperties} or
 * {@link PemSslBundleProperties}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class PropertiesSslBundle implements SslBundle {

  private final SslStoreBundle stores;

  private final SslBundleKey key;

  private final SslOptions options;

  private final String protocol;

  private final SslManagerBundle managers;

  private PropertiesSslBundle(SslStoreBundle stores, SslBundleProperties properties) {
    this.stores = stores;
    this.key = asSslKeyReference(properties.getKey());
    this.options = asSslOptions(properties.getOptions());
    this.protocol = properties.getProtocol();
    this.managers = SslManagerBundle.from(this.stores, this.key);
  }

  private static SslBundleKey asSslKeyReference(@Nullable SslBundleProperties.Key key) {
    return (key != null) ? SslBundleKey.of(key.getPassword(), key.getAlias()) : SslBundleKey.NONE;
  }

  private static SslOptions asSslOptions(@Nullable SslBundleProperties.Options options) {
    return (options != null) ? SslOptions.of(options.getCiphers(), options.getEnabledProtocols()) : SslOptions.NONE;
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
   * Get an {@link SslBundle} for the given {@link PemSslBundleProperties}.
   *
   * @param properties the source properties
   * @return an {@link SslBundle} instance
   */
  public static SslBundle get(PemSslBundleProperties properties) {
    return get(properties, ApplicationResourceLoader.of());
  }

  /**
   * Get an {@link SslBundle} for the given {@link PemSslBundleProperties}.
   *
   * @param properties the source properties
   * @param resourceLoader the resource loader used to load content
   * @return an {@link SslBundle} instance
   * @since 5.0
   */
  public static SslBundle get(PemSslBundleProperties properties, ResourceLoader resourceLoader) {
    PemSslStore keyStore = getPemSslStore("keystore", properties.getKeystore(), resourceLoader);
    if (keyStore != null) {
      keyStore = keyStore.withAlias(properties.getKey().getAlias())
              .withPassword(properties.getKey().getPassword());
    }
    PemSslStore trustStore = getPemSslStore("truststore", properties.getTruststore(), resourceLoader);
    SslStoreBundle storeBundle = new PemSslStoreBundle(keyStore, trustStore);
    return new PropertiesSslBundle(storeBundle, properties);
  }

  @Nullable
  private static PemSslStore getPemSslStore(String propertyName, PemSslBundleProperties.Store properties, ResourceLoader resourceLoader) {
    PemSslStore pemSslStore = PemSslStore.load(asPemSslStoreDetails(properties), resourceLoader);
    if (properties.isVerifyKeys()) {
      CertificateMatcher certificateMatcher = new CertificateMatcher(pemSslStore.privateKey());
      Assert.state(certificateMatcher.matchesAny(pemSslStore.certificates()),
              "Private key in %s matches none of the certificates in the chain".formatted(propertyName));
    }
    return pemSslStore;
  }

  private static PemSslStoreDetails asPemSslStoreDetails(PemSslBundleProperties.Store properties) {
    return new PemSslStoreDetails(properties.getType(), properties.getCertificate(), properties.getPrivateKey(),
            properties.getPrivateKeyPassword());
  }

  /**
   * Get an {@link SslBundle} for the given {@link JksSslBundleProperties}.
   *
   * @param properties the source properties
   * @return an {@link SslBundle} instance
   */
  public static SslBundle get(JksSslBundleProperties properties) {
    return get(properties, ApplicationResourceLoader.of());
  }

  /**
   * Get an {@link SslBundle} for the given {@link JksSslBundleProperties}.
   *
   * @param properties the source properties
   * @param resourceLoader the resource loader used to load content
   * @return an {@link SslBundle} instance
   * @since 5.0
   */
  public static SslBundle get(JksSslBundleProperties properties, ResourceLoader resourceLoader) {
    SslStoreBundle storeBundle = asSslStoreBundle(properties, resourceLoader);
    return new PropertiesSslBundle(storeBundle, properties);
  }

  private static SslStoreBundle asSslStoreBundle(JksSslBundleProperties properties, ResourceLoader resourceLoader) {
    JksSslStoreDetails keyStoreDetails = asStoreDetails(properties.getKeystore());
    JksSslStoreDetails trustStoreDetails = asStoreDetails(properties.getTruststore());
    return new JksSslStoreBundle(keyStoreDetails, trustStoreDetails, resourceLoader);
  }

  private static JksSslStoreDetails asStoreDetails(JksSslBundleProperties.Store properties) {
    return new JksSslStoreDetails(properties.getType(), properties.getProvider(), properties.getLocation(),
            properties.getPassword());
  }

  @Override
  public String toString() {
    ToStringBuilder creator = new ToStringBuilder(this);
    creator.append("key", this.key);
    creator.append("options", this.options);
    creator.append("protocol", this.protocol);
    creator.append("stores", this.stores);
    return creator.toString();
  }

}
