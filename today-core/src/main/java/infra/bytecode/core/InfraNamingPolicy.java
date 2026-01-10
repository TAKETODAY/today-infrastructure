/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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