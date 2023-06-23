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

package cn.taketoday.aot.agent;

import cn.taketoday.aot.hint.JavaSerializationHint;
import cn.taketoday.aot.hint.JdkProxyHint;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.ResourceBundleHint;
import cn.taketoday.aot.hint.ResourcePatternHint;

/**
 * Main types of {@link cn.taketoday.aot.hint.RuntimeHints}.
 *
 * <p>This allows to sort {@linkplain RecordedInvocation recorded invocations}
 * into hint categories.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public enum HintType {

  /**
   * Reflection hint, as described by {@link cn.taketoday.aot.hint.ReflectionHints}.
   */
  REFLECTION(ReflectionHints.class),

  /**
   * Resource pattern hint, as described by {@link cn.taketoday.aot.hint.ResourceHints#resourcePatternHints()}.
   */
  RESOURCE_PATTERN(ResourcePatternHint.class),

  /**
   * Resource bundle hint, as described by {@link cn.taketoday.aot.hint.ResourceHints#resourceBundleHints()}.
   */
  RESOURCE_BUNDLE(ResourceBundleHint.class),

  /**
   * Java serialization hint, as described by {@link cn.taketoday.aot.hint.JavaSerializationHint}.
   */
  JAVA_SERIALIZATION(JavaSerializationHint.class),

  /**
   * JDK proxies hint, as described by {@link cn.taketoday.aot.hint.ProxyHints#jdkProxyHints()}.
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
