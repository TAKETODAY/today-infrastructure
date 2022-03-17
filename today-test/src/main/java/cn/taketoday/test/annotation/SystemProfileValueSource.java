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

package cn.taketoday.test.annotation;

import cn.taketoday.lang.Assert;

/**
 * Implementation of {@link ProfileValueSource} which uses system properties as
 * the underlying source.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 */
public final class SystemProfileValueSource implements ProfileValueSource {

  private static final SystemProfileValueSource INSTANCE = new SystemProfileValueSource();

  /**
   * Obtain the canonical instance of this ProfileValueSource.
   */
  public static final SystemProfileValueSource getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor, enforcing the singleton pattern.
   */
  private SystemProfileValueSource() {
  }

  /**
   * Get the <em>profile value</em> indicated by the specified key from the
   * system properties.
   *
   * @see System#getProperty(String)
   */
  @Override
  public String get(String key) {
    Assert.hasText(key, "'key' must not be empty");
    return System.getProperty(key);
  }

}
