/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.annotation.config.ssl.SslBundleProperties.Key;
import cn.taketoday.annotation.config.ssl.SslBundleProperties.Options;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleKey;
import cn.taketoday.core.ssl.SslManagerBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.core.ssl.SslStoreBundle;
import cn.taketoday.core.ssl.jks.JksSslStoreBundle;
import cn.taketoday.core.ssl.jks.JksSslStoreDetails;
import cn.taketoday.core.ssl.pem.PemSslStoreBundle;
import cn.taketoday.core.ssl.pem.PemSslStoreDetails;
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

  private static SslOptions asSslOptions(@Nullable Options options) {
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
    return new PropertiesSslBundle(asSslStoreBundle(properties), properties);
  }

  /**
   * Get an {@link SslBundle} for the given {@link JksSslBundleProperties}.
   *
   * @param properties the source properties
   * @return an {@link SslBundle} instance
   */
  public static SslBundle get(JksSslBundleProperties properties) {
    return new PropertiesSslBundle(asSslStoreBundle(properties), properties);
  }

  private static SslStoreBundle asSslStoreBundle(PemSslBundleProperties properties) {
    PemSslStoreDetails keyStoreDetails = asStoreDetails(properties.getKeystore());
    PemSslStoreDetails trustStoreDetails = asStoreDetails(properties.getTruststore());
    return new PemSslStoreBundle(keyStoreDetails, trustStoreDetails, properties.getKey().getAlias(), null,
            properties.isVerifyKeys());
  }

  private static PemSslStoreDetails asStoreDetails(PemSslBundleProperties.Store properties) {
    return new PemSslStoreDetails(properties.getType(), properties.getCertificate(),
            properties.getPrivateKey(), properties.getPrivateKeyPassword());
  }

  private static SslStoreBundle asSslStoreBundle(JksSslBundleProperties properties) {
    JksSslStoreDetails keyStoreDetails = asStoreDetails(properties.getKeystore());
    JksSslStoreDetails trustStoreDetails = asStoreDetails(properties.getTruststore());
    return new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
  }

  private static JksSslStoreDetails asStoreDetails(JksSslBundleProperties.Store properties) {
    return new JksSslStoreDetails(properties.getType(), properties.getProvider(),
            properties.getLocation(), properties.getPassword());
  }

}
