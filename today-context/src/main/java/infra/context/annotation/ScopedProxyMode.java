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

package infra.context.annotation;

/**
 * Enumerates the various scoped-proxy options.
 *
 * <p>For a more complete discussion of exactly what a scoped proxy is, see the
 * section of the Framework reference documentation entitled '<em>Scoped beans as
 * dependencies</em>'.
 *
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ScopeMetadata
 * @since 4.0 2022/3/7 21:31
 */
public enum ScopedProxyMode {

  /**
   * Default typically equals {@link #NO}, unless a different default
   * has been configured at the component-scan instruction level.
   */
  DEFAULT,

  /**
   * Do not create a scoped proxy.
   * <p>This proxy-mode is not typically useful when used with a
   * non-singleton scoped instance, which should favor the use of the
   * {@link #INTERFACES} or {@link #TARGET_CLASS} proxy-modes instead if it
   * is to be used as a dependency.
   */
  NO,

  /**
   * Create a JDK dynamic proxy implementing <i>all</i> interfaces exposed by
   * the class of the target object.
   */
  INTERFACES,

  /**
   * Create a class-based proxy (uses CGLIB).
   */
  TARGET_CLASS

}
