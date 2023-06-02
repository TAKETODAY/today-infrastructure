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

package cn.taketoday.core.ssl.jks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import cn.taketoday.core.ssl.SslStoreBundle;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link SslStoreBundle} backed by a Java keystore.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 4.0
 */
public class JksSslStoreBundle implements SslStoreBundle {

  @Nullable
  private final JksSslStoreDetails keyStoreDetails;

  @Nullable
  private final JksSslStoreDetails trustStoreDetails;

  /**
   * Create a new {@link JksSslStoreBundle} instance.
   *
   * @param keyStoreDetails the key store details
   * @param trustStoreDetails the trust store details
   */
  public JksSslStoreBundle(@Nullable JksSslStoreDetails keyStoreDetails, @Nullable JksSslStoreDetails trustStoreDetails) {
    this.keyStoreDetails = keyStoreDetails;
    this.trustStoreDetails = trustStoreDetails;
  }

  @Override
  @Nullable
  public KeyStore getKeyStore() {
    return createKeyStore("key", this.keyStoreDetails);
  }

  @Override
  @Nullable
  public String getKeyStorePassword() {
    return (this.keyStoreDetails != null) ? this.keyStoreDetails.password() : null;
  }

  @Override
  @Nullable
  public KeyStore getTrustStore() {
    return createKeyStore("trust", this.trustStoreDetails);
  }

  @Nullable
  private KeyStore createKeyStore(String name, @Nullable JksSslStoreDetails details) {
    if (details == null || details.isEmpty()) {
      return null;
    }
    try {
      String type = (!StringUtils.hasText(details.type())) ? KeyStore.getDefaultType() : details.type();
      char[] password = (details.password() != null) ? details.password().toCharArray() : null;
      String location = details.location();
      KeyStore store = getKeyStoreInstance(type, details.provider());
      if (isHardwareKeystoreType(type)) {
        loadHardwareKeyStore(store, location, password);
      }
      else {
        loadKeyStore(store, location, password);
      }
      return store;
    }
    catch (Exception ex) {
      throw new IllegalStateException("Unable to create %s store: %s".formatted(name, ex.getMessage()), ex);
    }
  }

  private KeyStore getKeyStoreInstance(String type, @Nullable String provider)
          throws KeyStoreException, NoSuchProviderException {
    return (!StringUtils.hasText(provider)) ? KeyStore.getInstance(type) : KeyStore.getInstance(type, provider);
  }

  private boolean isHardwareKeystoreType(String type) {
    return type.equalsIgnoreCase("PKCS11");
  }

  private void loadHardwareKeyStore(KeyStore store, @Nullable String location, @Nullable char[] password)
          throws IOException, NoSuchAlgorithmException, CertificateException {
    if (StringUtils.hasText(location)) {
      throw new IllegalStateException(
              "Location is '%s', but must be empty or null for PKCS11 hardware key stores".formatted(location));
    }
    store.load(null, password);
  }

  private void loadKeyStore(KeyStore store, @Nullable String location, @Nullable char[] password) {
    Assert.state(StringUtils.hasText(location), "Location must not be empty or null");
    try {
      URL url = ResourceUtils.getURL(location);
      try (InputStream stream = url.openStream()) {
        store.load(stream, password);
      }
    }
    catch (Exception ex) {
      throw new IllegalStateException("Could not load store from '" + location + "'", ex);
    }
  }

}
