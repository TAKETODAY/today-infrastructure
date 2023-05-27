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

package cn.taketoday.framework.web.server;

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

  private WebServerSslBundle(SslStoreBundle stores, String keyPassword, Ssl ssl) {
    this.stores = stores;
    this.key = SslBundleKey.of(keyPassword, ssl.getKeyAlias());
    this.protocol = ssl.getProtocol();
    this.options = SslOptions.of(ssl.getCiphers(), ssl.getEnabledProtocols());
    this.managers = SslManagerBundle.from(this.stores, this.key);
  }

  private static SslStoreBundle createPemStoreBundle(Ssl ssl) {
    PemSslStoreDetails keyStoreDetails = new PemSslStoreDetails(
            ssl.getKeyStoreType(), ssl.getCertificate(), ssl.getCertificatePrivateKey());
    PemSslStoreDetails trustStoreDetails = new PemSslStoreDetails(ssl.getTrustStoreType(),
            ssl.getTrustCertificate(), ssl.getTrustCertificatePrivateKey());
    return new PemSslStoreBundle(keyStoreDetails, trustStoreDetails, ssl.getKeyAlias());
  }

  private static SslStoreBundle createJksStoreBundle(Ssl ssl) {
    JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(ssl.getKeyStoreType(), ssl.getKeyStoreProvider(),
            ssl.getKeyStore(), ssl.getKeyStorePassword());
    JksSslStoreDetails trustStoreDetails = new JksSslStoreDetails(ssl.getTrustStoreType(),
            ssl.getTrustStoreProvider(), ssl.getTrustStore(), ssl.getTrustStorePassword());
    return new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
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
   * @param sslBundles the bundles that should be used when {@link Ssl#getBundle()} is
   * set
   * @return a {@link SslBundle} instance
   * @throws NoSuchSslBundleException if a bundle lookup fails
   */
  public static SslBundle get(Ssl ssl, @Nullable SslBundles sslBundles) throws NoSuchSslBundleException {
    Assert.state(Ssl.isEnabled(ssl), "SSL is not enabled");
    String bundleName = ssl.getBundle();
    if (StringUtils.hasText(bundleName)) {
      if (sslBundles == null) {
        throw new IllegalStateException(
                "SSL bundle '%s' was requested but no SslBundles instance was provided".formatted(bundleName));
      }
      return sslBundles.getBundle(bundleName);
    }
    SslStoreBundle stores = createStoreBundle(ssl);
    String keyPassword = ssl.getKeyPassword();
    return new WebServerSslBundle(stores, keyPassword, ssl);
  }

  private static SslStoreBundle createStoreBundle(Ssl ssl) {
    if (hasCertificateProperties(ssl)) {
      return createPemStoreBundle(ssl);
    }
    if (hasJavaKeyStoreProperties(ssl)) {
      return createJksStoreBundle(ssl);
    }
    throw new IllegalStateException("SSL is enabled but no trust material is configured");
  }

  @Nullable
  static SslBundle createCertificateFileSslStoreProviderDelegate(Ssl ssl) {
    if (!hasCertificateProperties(ssl)) {
      return null;
    }
    SslStoreBundle stores = createPemStoreBundle(ssl);
    return new WebServerSslBundle(stores, ssl.getKeyPassword(), ssl);
  }

  private static boolean hasCertificateProperties(Ssl ssl) {
    return Ssl.isEnabled(ssl) && ssl.getCertificate() != null && ssl.getCertificatePrivateKey() != null;
  }

  private static boolean hasJavaKeyStoreProperties(Ssl ssl) {
    return Ssl.isEnabled(ssl) && ssl.getKeyStore() != null
            || (ssl.getKeyStoreType() != null && ssl.getKeyStoreType().equals("PKCS11"));
  }

}
