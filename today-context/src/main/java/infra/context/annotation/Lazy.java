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

package infra.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.factory.BeanFactory;
import infra.stereotype.Component;

/**
 * Indicates whether a bean is to be lazily initialized.
 *
 * <p>May be used on any class directly or indirectly annotated with
 * {@link Component @Component} or on methods annotated with {@link Component @Component}.
 *
 * <p>If this annotation is not present on a {@code @Component} or {@code @Bean} definition,
 * eager initialization will occur. If present and set to {@code true}, the {@code @Component} or
 * {@code @Component} will not be initialized until referenced by another bean or explicitly
 * retrieved from the enclosing {@link BeanFactory
 * BeanFactory}. If present and set to {@code false}, the bean will be instantiated on
 * startup by bean factories that perform eager initialization of singletons.
 *
 * <p>If Lazy is present on a {@link Configuration @Configuration} class, this
 * indicates that all {@code @Component} methods within that {@code @Configuration}
 * should be lazily initialized. If {@code @Lazy} is present and false on a {@code @Component}
 * method within a {@code @Lazy}-annotated {@code @Configuration} class, this indicates
 * overriding the 'default lazy' behavior and that the bean should be eagerly initialized.
 *
 * <p>In addition to its role for component initialization, this annotation may also be placed
 * on injection points marked with {@link infra.beans.factory.annotation.Autowired}
 * or {@link jakarta.inject.Inject}: In that context, it leads to the creation of a
 * lazy-resolution proxy for the affected dependency, caching it on first access in case of
 * a singleton or re-resolving it on every access otherwise. This is an alternative to using
 * {@link java.util.function.Supplier} or {@link jakarta.inject.Provider}.
 * Please note that such a lazy-resolution proxy will always be injected; if the target
 * dependency does not exist, you will only be able to find out through an exception on
 * invocation. As a consequence, such an injection point results in unintuitive behavior
 * for optional dependencies. For a programmatic equivalent, allowing for lazy references
 * with more sophistication, consider {@link infra.beans.factory.ObjectProvider}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author TODAY 2021/3/9 12:09
 * @see Primary
 * @see Configuration
 * @see Component
 * @since 3.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Lazy {

  /**
   * Whether lazy initialization should occur.
   */
  boolean value() default true;

}
