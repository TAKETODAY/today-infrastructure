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

package cn.taketoday.annotation.config.ssl;

import cn.taketoday.annotation.config.ssl.SslBundleProperties.Key;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleKey;
import cn.taketoday.core.ssl.SslManagerBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.core.ssl.SslStoreBundle;
import cn.taketoday.core.ssl.jks.JksSslStoreBundle;
import cn.taketoday.core.ssl.jks.JksSslStoreDetails;
import cn.taketoday.core.ssl.pem.PemSslStore;
import cn.taketoday.core.ssl.pem.PemSslStoreBundle;
import cn.taketoday.core.ssl.pem.PemSslStoreDetails;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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

  private static SslBundleKey asSslKeyReference(@Nullable Key key) {
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
    PemSslStore keyStore = getPemSslStore("keystore", properties.getKeystore());
    if (keyStore != null) {
      keyStore = keyStore.withAlias(properties.getKey().getAlias())
              .withPassword(properties.getKey().getPassword());
    }
    PemSslStore trustStore = getPemSslStore("truststore", properties.getTruststore());
    SslStoreBundle storeBundle = new PemSslStoreBundle(keyStore, trustStore);
    return new PropertiesSslBundle(storeBundle, properties);
  }

  @Nullable
  private static PemSslStore getPemSslStore(String propertyName, PemSslBundleProperties.Store properties) {
    PemSslStore pemSslStore = PemSslStore.load(asPemSslStoreDetails(properties));
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
    SslStoreBundle storeBundle = asSslStoreBundle(properties);
    return new PropertiesSslBundle(storeBundle, properties);
  }

  private static SslStoreBundle asSslStoreBundle(JksSslBundleProperties properties) {
    JksSslStoreDetails keyStoreDetails = asStoreDetails(properties.getKeystore());
    JksSslStoreDetails trustStoreDetails = asStoreDetails(properties.getTruststore());
    return new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
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
