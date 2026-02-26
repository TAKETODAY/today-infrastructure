/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core;

import java.io.Console;
import java.io.Reader;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.Future;

import infra.util.ReflectionUtils;

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
  EIGHTEEN("18", Duration.class, "isPositive"),

  /**
   * Java 19.
   */
  NINETEEN("19", Future.class, "state"),

  /**
   * Java 20.
   */
  TWENTY("20", Class.class, "accessFlags"),

  /**
   * Java 21.
   */
  TWENTY_ONE("21", SortedSet.class, "getFirst"),

  /**
   * Java 22.
   */
  TWENTY_TWO("22", Console.class, "isTerminal"),

  /**
   * Java 23.
   *
   * @since 5.0
   */
  TWENTY_THREE("23", NumberFormat.class, "isStrict"),

  /**
   * Java 24.
   *
   * @since 5.0
   */
  TWENTY_FOUR("24", Reader.class, "of", CharSequence.class),

  /**
   * Java 25.
   *
   * @since 5.0
   */
  TWENTY_FIVE("25", Reader.class, "readAllLines"),

  /**
   * Java 26.
   *
   * @since 5.0
   */
  TWENTY_SIX("26", String.class, "equalsFoldCase", String.class);

  private final String name;

  private final boolean available;

  JavaVersion(String name, Class<?> versionSpecificClass, String versionSpecificMethod, Class<?>... paramTypes) {
    this.name = name;
    this.available = ReflectionUtils.hasMethod(versionSpecificClass, versionSpecificMethod, paramTypes);
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
  public static JavaVersion current() {
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
