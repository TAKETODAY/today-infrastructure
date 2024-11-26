/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.aot.agent;

import infra.aot.hint.JavaSerializationHint;
import infra.aot.hint.JdkProxyHint;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.ResourceBundleHint;
import infra.aot.hint.ResourcePatternHint;

/**
 * Main types of {@link infra.aot.hint.RuntimeHints}.
 *
 * <p>This allows to sort {@linkplain RecordedInvocation recorded invocations}
 * into hint categories.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public enum HintType {

  /**
   * Reflection hint, as described by {@link infra.aot.hint.ReflectionHints}.
   */
  REFLECTION(ReflectionHints.class),

  /**
   * Resource pattern hint, as described by {@link infra.aot.hint.ResourceHints#resourcePatternHints()}.
   */
  RESOURCE_PATTERN(ResourcePatternHint.class),

  /**
   * Resource bundle hint, as described by {@link infra.aot.hint.ResourceHints#resourceBundleHints()}.
   */
  RESOURCE_BUNDLE(ResourceBundleHint.class),

  /**
   * Java serialization hint, as described by {@link infra.aot.hint.JavaSerializationHint}.
   */
  JAVA_SERIALIZATION(JavaSerializationHint.class),

  /**
   * JDK proxies hint, as described by {@link infra.aot.hint.ProxyHints#jdkProxyHints()}.
   */
  JDK_PROXIES(JdkProxyHint.class);

  private final Class<?> hintClass;

  HintType(Class<?> hintClass) {
    this.hintClass = hintClass;
  }

  public String hintClassName() {
    return this.hintClass.getSimpleName();
  }

}
