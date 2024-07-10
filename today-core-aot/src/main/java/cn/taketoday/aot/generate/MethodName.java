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

package cn.taketoday.aot.generate;

import java.util.Arrays;
import java.util.stream.Collectors;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * A camel-case method name that can be built from distinct parts.
 *
 * @author Phillip Webb
 * @since 4.0
 */
final class MethodName {

  private static final String[] PREFIXES = { "get", "set", "is" };

  /**
   * An empty method name.
   */
  public static final MethodName NONE = of();

  private final String value;

  private MethodName(String value) {
    this.value = value;
  }

  /**
   * Create a new method name from the specific parts. The returned name will
   * be in camel-case and will only contain valid characters from the parts.
   *
   * @param parts the parts the form the name
   * @return a method name instance
   */
  static MethodName of(String... parts) {
    Assert.notNull(parts, "'parts' is required");
    return new MethodName(join(parts));
  }

  /**
   * Create a new method name by concatenating the specified name to this name.
   *
   * @param name the name to concatenate
   * @return a new method name instance
   */
  MethodName and(MethodName name) {
    Assert.notNull(name, "'name' is required");
    return and(name.value);
  }

  /**
   * Create a new method name by concatenating the specified parts to this name.
   *
   * @param parts the parts to concatenate
   * @return a new method name instance
   */
  MethodName and(String... parts) {
    Assert.notNull(parts, "'parts' is required");
    String joined = join(parts);
    String prefix = getPrefix(joined);
    String suffix = joined.substring(prefix.length());
    return of(prefix, this.value, suffix);
  }

  private String getPrefix(String name) {
    for (String candidate : PREFIXES) {
      if (name.startsWith(candidate)) {
        return candidate;
      }
    }
    return "";
  }

  @Override
  public int hashCode() {
    return this.value.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }
    return this.value.equals(((MethodName) obj).value);
  }

  @Override
  public String toString() {
    return (StringUtils.isEmpty(this.value)) ? "$$aot" : this.value;
  }

  private static String join(String[] parts) {
    return StringUtils.uncapitalize(Arrays.stream(parts).map(MethodName::clean)
            .map(StringUtils::capitalize).collect(Collectors.joining()));
  }

  private static String clean(@Nullable String part) {
    char[] chars = (part != null) ? part.toCharArray() : new char[0];
    StringBuilder name = new StringBuilder(chars.length);
    boolean uppercase = false;
    for (char ch : chars) {
      char outputChar = (!uppercase) ? ch : Character.toUpperCase(ch);
      name.append((!Character.isLetter(ch)) ? "" : outputChar);
      uppercase = (ch == '.');
    }
    return name.toString();
  }

}
