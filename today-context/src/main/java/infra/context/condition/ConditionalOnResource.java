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

package infra.context.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Conditional;

/**
 * {@link Conditional} that only matches when the specified resources are exits
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-06-18 15:07
 */
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnResourceCondition.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalOnResource {

  /**
   * The resources that must be present.
   *
   * @return the resource paths that must be present.
   */
  String[] value() default {};

}

