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
import infra.web.RequestContext;

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
 * @see RequestContext#SCOPE_REQUEST
 * @see Component
 * @see Bean
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(RequestContext.SCOPE_REQUEST)
public @interface RequestScope {

  /**
   * Alias for {@link Scope#proxyMode}.
   * <p>Defaults to {@link ScopedProxyMode#TARGET_CLASS}.
   */
  @AliasFor(annotation = Scope.class)
  ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;

}
