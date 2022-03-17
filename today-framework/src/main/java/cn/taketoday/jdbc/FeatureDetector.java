/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc;

import cn.taketoday.util.ClassUtils;

/**
 * Detects whether optional features are available.
 *
 * @author Alden Quimby
 */
@SuppressWarnings("UnusedDeclaration")
public final class FeatureDetector {

  static {
    setCacheUnderscoreToCamelcaseEnabled(true); // enabled by default
  }

  private static boolean cacheUnderscoreToCamelcaseEnabled;
  private static final boolean oracleAvailable = ClassUtils.isPresent("oracle.sql.TIMESTAMP");
  private static final boolean jodaTimeAvailable = ClassUtils.isPresent("org.joda.time.DateTime");

  /**
   * @return {@code true} if Joda-Time is available, {@code false} otherwise.
   */
  public static boolean isJodaTimeAvailable() {
    return jodaTimeAvailable;
  }

  /**
   * @return {@code true} if oracle.sql is available, {@code false} otherwise.
   */
  public static boolean isOracleAvailable() {
    return oracleAvailable;
  }

  /**
   * @return {@code true} if caching of underscore to camelcase is enabled.
   */
  public static boolean isCacheUnderscoreToCamelcaseEnabled() {
    return cacheUnderscoreToCamelcaseEnabled;
  }

  /**
   * Turn caching of underscore to camelcase on or off.
   */
  public static void setCacheUnderscoreToCamelcaseEnabled(boolean cacheUnderscoreToCamelcaseEnabled) {
    FeatureDetector.cacheUnderscoreToCamelcaseEnabled = cacheUnderscoreToCamelcaseEnabled;
  }
}
