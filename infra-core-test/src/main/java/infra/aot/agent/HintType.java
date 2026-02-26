/*
 * Copyright 2002-present the original author or authors.
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

package infra.aot.agent;

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
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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
