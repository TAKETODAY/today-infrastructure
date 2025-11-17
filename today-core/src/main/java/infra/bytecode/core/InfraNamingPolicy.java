/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.bytecode.core;

import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

final class InfraNamingPolicy implements NamingPolicy {

  public static final InfraNamingPolicy INSTANCE = new InfraNamingPolicy();

  private static final String LABEL = "$$Infra$$";

  private static final String METHOD_ACCESS_SUFFIX = "MethodAccess$$";

  @Override
  public String getClassName(@Nullable String prefix, String source, Object key, Predicate<String> names) {
    if (prefix == null) {
      prefix = "infra.bytecode.Object";
    }
    else if (prefix.startsWith("java.") || prefix.startsWith("javax.")) {
      prefix = "_" + prefix;
    }

    String base;
    int existingLabel = prefix.indexOf(LABEL);
    if (existingLabel >= 0) {
      base = prefix.substring(0, existingLabel + LABEL.length());
    }
    else {
      base = prefix + LABEL;
    }

    // When the generated class name is for a FastClass, the source is
    // "infra.bytecode.reflect.MethodAccess".
    boolean isMethodAccess = (source != null && source.endsWith("MethodAccess"));
    if (isMethodAccess && !prefix.contains(METHOD_ACCESS_SUFFIX)) {
      base += METHOD_ACCESS_SUFFIX;
    }

    int index = 0;
    String attempt = base + index;
    while (names.test(attempt)) {
      attempt = base + index++;
    }
    return attempt;
  }

}