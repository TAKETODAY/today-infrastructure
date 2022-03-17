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

import java.security.KeyStore;
import java.security.KeyStoreException;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Provides utilities around SSL.
 *
 * @author Chris Bono
 * @since 4.0
 */
public final class SslConfigurationValidator {

  private SslConfigurationValidator() {
  }

  public static void validateKeyAlias(KeyStore keyStore, String keyAlias) {
    if (StringUtils.isNotEmpty(keyAlias)) {
      try {
        Assert.state(keyStore.containsAlias(keyAlias),
                () -> String.format("Keystore does not contain specified alias '%s'", keyAlias));
      }
      catch (KeyStoreException ex) {
        throw new IllegalStateException(
                String.format("Could not determine if keystore contains alias '%s'", keyAlias), ex);
      }
    }
  }

}
