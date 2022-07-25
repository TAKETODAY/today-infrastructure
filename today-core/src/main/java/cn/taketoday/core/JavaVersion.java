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

package cn.taketoday.core;

import java.io.Console;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.util.ReflectionUtils;

/**
 * Known Java versions.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/4 12:23
 */
public enum JavaVersion {

  /**
   * Java 17.
   */
  SEVENTEEN("17", Console.class, "charset"),

  /**
   * Java 18.
   */
  EIGHTEEN("18", Duration.class, "isPositive");

  private final String name;

  private final boolean available;

  JavaVersion(String name, Class<?> clazz, String methodName) {
    this.name = name;
    this.available = ReflectionUtils.hasMethod(clazz, methodName);
  }

  @Override
  public String toString() {
    return this.name;
  }

  /**
   * Returns the {@link JavaVersion} of the current runtime.
   *
   * @return the {@link JavaVersion}
   */
  public static JavaVersion getJavaVersion() {
    List<JavaVersion> candidates = Arrays.asList(JavaVersion.values());
    Collections.reverse(candidates);
    for (JavaVersion candidate : candidates) {
      if (candidate.available) {
        return candidate;
      }
    }
    return SEVENTEEN;
  }

  /**
   * Return if this version is equal to or newer than a given version.
   *
   * @param version the version to compare
   * @return {@code true} if this version is equal to or newer than {@code version}
   */
  public boolean isEqualOrNewerThan(JavaVersion version) {
    return compareTo(version) >= 0;
  }

  /**
   * Return if this version is older than a given version.
   *
   * @param version the version to compare
   * @return {@code true} if this version is older than {@code version}
   */
  public boolean isOlderThan(JavaVersion version) {
    return compareTo(version) < 0;
  }

}
