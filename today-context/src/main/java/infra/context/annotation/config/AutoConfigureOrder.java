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

package infra.context.annotation.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.DependsOn;
import infra.core.Ordered;
import infra.core.annotation.Order;

/**
 * Auto-configuration specific variant of Infra {@link Order @Order}
 * annotation. Allows auto-configuration classes to be ordered among themselves without
 * affecting the order of configuration classes passed to
 * {@link AnnotationConfigApplicationContext#register(Class...)}.
 * <p>
 * As with standard {@link Configuration @Configuration} classes, the order in which
 * auto-configuration classes are applied only affects the order in which their beans are
 * defined. The order in which those beans are subsequently created is unaffected and is
 * determined by each bean's dependencies and any {@link DependsOn @DependsOn}
 * relationships.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 11:56
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface AutoConfigureOrder {

  /**
   * The default order value.
   */
  int DEFAULT_ORDER = 0;

  /**
   * The order value. Default is {@code 0}.
   *
   * @return the order value
   * @see Ordered#getOrder()
   */
  int value() default DEFAULT_ORDER;

}

