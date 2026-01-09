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

package infra.web.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Bean;
import infra.context.annotation.Scope;
import infra.context.annotation.ScopedProxyMode;
import infra.core.annotation.AliasFor;
import infra.stereotype.Component;

/**
 * {@code @RequestScope} is a specialization of {@link Scope @Scope} for a
 * component whose lifecycle is bound to the current web request.
 *
 * <p>{@code @RequestScope} may be used as a meta-annotation to create custom
 * composed annotations.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SessionScope
 * @see Scope
 * @see Component
 * @see Bean
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(RequestScope.NAME)
public @interface RequestScope {

  /**
   * Scope identifier for request scope: "request".
   * Supported in addition to the standard scopes "singleton" and "prototype".
   */
  String NAME = "request";

  /**
   * Alias for {@link Scope#proxyMode}.
   * <p>Defaults to {@link ScopedProxyMode#TARGET_CLASS}.
   */
  @AliasFor(annotation = Scope.class)
  ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;

}
