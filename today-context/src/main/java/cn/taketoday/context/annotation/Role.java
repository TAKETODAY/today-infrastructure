/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.config.BeanDefinition;

/**
 * Indicates the 'role' hint for a given bean.
 *
 * <p>May be used on any class directly or indirectly annotated with
 * {@link cn.taketoday.stereotype.Component}
 *
 * <p>If this annotation is not present on a Component or Bean definition,
 * the default value of {@link BeanDefinition#ROLE_APPLICATION} will apply.
 *
 * <p>If Role is present on a {@link Configuration @Configuration} class,
 * this indicates the role of the configuration class bean definition and
 * does not cascade to all @{@code Bean} methods defined within. This behavior
 * is different than that of the @{@link Lazy} annotation, for example.
 *
 * @author Chris Beams
 * @author Harry Yang 2021/10/14 14:26
 * @see BeanDefinition#ROLE_APPLICATION
 * @see BeanDefinition#ROLE_INFRASTRUCTURE
 * @see cn.taketoday.stereotype.Component
 * @since 4.0
 */
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Role {

  /**
   * Set the role hint for the associated bean.
   *
   * @see BeanDefinition#ROLE_APPLICATION
   * @see BeanDefinition#ROLE_INFRASTRUCTURE
   */
  int value();

}
