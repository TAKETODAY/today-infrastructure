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
 * {@code @SessionScope} is a specialization of {@link Scope @Scope} for a
 * component whose lifecycle is bound to the current web session.
 *
 * <p>{@code @SessionScope} may be used as a meta-annotation to create custom
 * composed annotations.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestScope
 * @see Scope
 * @see Component
 * @see infra.web.context.support.SessionScope
 * @see Bean
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Scope(SessionScope.NAME)
public @interface SessionScope {

  /**
   * Scope identifier for session scope: "session".
   * Supported in addition to the standard scopes "singleton" and "prototype".
   */
  String NAME = "session";

  /**
   * Alias for {@link Scope#proxyMode}.
   * <p>Defaults to {@link ScopedProxyMode#TARGET_CLASS}.
   */
  @AliasFor(annotation = Scope.class)
  ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;

}
