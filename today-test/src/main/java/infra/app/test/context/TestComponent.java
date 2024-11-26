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

package infra.app.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.ComponentScan;
import infra.context.annotation.config.TypeExcludeFilter;
import infra.core.annotation.AliasFor;
import infra.stereotype.Component;

/**
 * {@link Component @Component} that can be used when a bean is intended only for tests,
 * and should be excluded from Infra's component scanning.
 * <p>
 * Note that if you directly use {@link ComponentScan @ComponentScan} rather than relying
 * on {@code @InfraApplication} you should ensure that a {@link TypeExcludeFilter} is
 * declared as an {@link ComponentScan#excludeFilters() excludeFilter}.
 *
 * @author Phillip Webb
 * @see TypeExcludeFilter
 * @see TestConfiguration
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface TestComponent {

  /**
   * The value may indicate a suggestion for a logical component name, to be turned into
   * a Infra bean in case of an auto-detected component.
   *
   * @return the specified bean name, if any
   */
  @AliasFor(annotation = Component.class)
  String value() default "";

}
