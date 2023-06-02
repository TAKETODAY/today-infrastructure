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

import java.security.KeyStore;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Details for an individual trust or key store in a {@link JksSslStoreBundle}.
 *
 * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
 * {@code null} value will use {@link KeyStore#getDefaultType()}).
 * @param provider the name of the key store provider
 * @param location the location of the key store file or {@code null} if using a
 * {@code PKCS11} hardware store
 * @param password the password used to unlock the store or {@code null}
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 4.0
 */
public record JksSslStoreDetails(
        @Nullable String type, @Nullable String provider, @Nullable String location, @Nullable String password) {

  /**
   * Return a new {@link JksSslStoreDetails} instance with a new password.
   *
   * @param password the new password
   * @return a new {@link JksSslStoreDetails} instance
   */
  public JksSslStoreDetails withPassword(String password) {
    return new JksSslStoreDetails(this.type, this.provider, this.location, password);
  }

  boolean isEmpty() {
    return isEmpty(this.type) && isEmpty(this.provider) && isEmpty(this.location);
  }

  private boolean isEmpty(@Nullable String value) {
    return StringUtils.isBlank(value);
  }

  /**
   * Factory method to create a new {@link JksSslStoreDetails} instance for the given
   * location.
   *
   * @param location the location
   * @return a new {@link JksSslStoreDetails} instance.
   */
  public static JksSslStoreDetails forLocation(String location) {
    return new JksSslStoreDetails(null, null, location, null);
  }

}
