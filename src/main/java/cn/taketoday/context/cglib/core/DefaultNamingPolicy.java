/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.context.cglib.core;

import java.util.function.Predicate;

/**
 * The default policy used by {@link AbstractClassGenerator}. Generates names
 * such as
 * <p>
 * <code>cn.taketoday.context.cglib.Foo$$ByTODAY$$38272841</code>
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
  private static final boolean STRESS_HASH_CODE =
          Boolean.getBoolean("cn.taketoday.context.cglib.test.stressHashCodes");

  @Override
  public String getClassName(String prefix, String source, Object key, Predicate<String> names) {

    if (prefix == null) {
      prefix = "cn.taketoday.context.cglib.Object";
    }
    else if (prefix.startsWith("java")) {
      prefix = '$' + prefix;
    }

    final String base = new StringBuilder(prefix)//
            .append("$$")//
            .append(source)//
            .append(getTag())//
            .append("$$")//
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
    return (o instanceof DefaultNamingPolicy) && ((DefaultNamingPolicy) o).getTag().equals(getTag());
  }
}
