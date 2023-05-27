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

package cn.taketoday.core.ssl;

import cn.taketoday.lang.Nullable;

/**
 * Exception indicating that an {@link SslBundle} was referenced with a name that does not
 * match any registered bundle.
 *
 * @author Scott Frederick
 * @since 4.0
 */
public class NoSuchSslBundleException extends RuntimeException {

  private final String bundleName;

  /**
   * Create a new {@code SslBundleNotFoundException} instance.
   *
   * @param bundleName the name of the bundle that could not be found
   * @param message the exception message
   */
  public NoSuchSslBundleException(String bundleName, String message) {
    this(bundleName, message, null);
  }

  /**
   * Create a new {@code SslBundleNotFoundException} instance.
   *
   * @param bundleName the name of the bundle that could not be found
   * @param message the exception message
   * @param cause the exception cause
   */
  public NoSuchSslBundleException(String bundleName, String message, @Nullable Throwable cause) {
    super(message, cause);
    this.bundleName = bundleName;
  }

  /**
   * Return the name of the bundle that was not found.
   *
   * @return the bundle name
   */
  public String getBundleName() {
    return this.bundleName;
  }

}
