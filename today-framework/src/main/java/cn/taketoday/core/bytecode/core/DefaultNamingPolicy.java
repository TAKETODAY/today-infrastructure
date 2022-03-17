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
package cn.taketoday.core.bytecode.core;

import java.util.function.Predicate;

import cn.taketoday.lang.TodayStrategies;

/**
 * The default policy used by {@link AbstractClassGenerator}. Generates names
 * such as
 * <p>
 * <code>cn.taketoday.core.bytecode.Foo$$ByTODAY$$38272841</code>
 * <p>
 * This is composed of a prefix based on the name of the superclass, a fixed
 * string incorporating the CGLIB class responsible for generation, and a
 * hashcode derived from the parameters used to create the object. If the same
 * name has been previously been used in the same <code>ClassLoader</code>, a
 * suffix is added to ensure uniqueness.
 */
public class DefaultNamingPolicy implements NamingPolicy {

  public static final DefaultNamingPolicy INSTANCE = new DefaultNamingPolicy();

  /**
   * This allows to test collisions of {@code key.hashCode()}.
   */
  private static final boolean STRESS_HASH_CODE = TodayStrategies.getFlag("bytecode.stressHashCodes");

  @Override
  public String getClassName(String prefix, String source, Object key, Predicate<String> names) {
    if (prefix == null) {
      prefix = "cn.taketoday.core.bytecode.Object";
    }
    else if (prefix.startsWith("java")) {
      prefix = "system$" + prefix;
    }

    String base = new StringBuilder(prefix)
            .append("$$")
            .append(source)
            .append(getTag())
            .append("$$")
            .append(Integer.toHexString(STRESS_HASH_CODE ? 0 : key.hashCode())).toString();

    String attempt = base;
    int index = 2;
    while (names.test(attempt))
      attempt = base + '_' + index++;
    return attempt;
  }

  /**
   * Returns a string which is incorporated into every generated class name. By
   * default returns "ByTODAY"
   */
  protected String getTag() {
    return "ByTODAY";
  }

  public int hashCode() {
    return getTag().hashCode();
  }

  public boolean equals(Object o) {
    return o instanceof DefaultNamingPolicy policy && policy.getTag().equals(getTag());
  }
}
