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

package infra.aot.hint;

/**
 * Hints that can be used to optimize the application runtime.
 *
 * <p>The use of reflection can be recorded for individual members of a type,
 * lambdas, or broader {@linkplain MemberCategory member categories}.
 *
 * <p>Access to resources can be specified using patterns or the base name of a
 * resource bundle.
 *
 * <p>The need for Java serialization or proxies can be recorded as well.
 *
 * @author Stephane Nicoll
 * @author Janne Valkealahti
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class RuntimeHints {

  private final ReflectionHints reflection = new ReflectionHints();

  private final ResourceHints resources = new ResourceHints();

  private final ProxyHints proxies = new ProxyHints();

  private final ReflectionHints jni = new ReflectionHints();

  /**
   * Provide access to reflection-based hints.
   *
   * @return reflection hints
   */
  public ReflectionHints reflection() {
    return this.reflection;
  }

  /**
   * Provide access to resource-based hints.
   *
   * @return resource hints
   */
  public ResourceHints resources() {
    return this.resources;
  }

  /**
   * Provide access to proxy-based hints.
   *
   * @return proxy hints
   */
  public ProxyHints proxies() {
    return this.proxies;
  }

  /**
   * Provide access to jni-based hints.
   *
   * @return jni hints
   */
  public ReflectionHints jni() {
    return this.jni;
  }

}
