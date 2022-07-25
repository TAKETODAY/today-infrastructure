/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.naming.InitialContext;

import cn.taketoday.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches based on the availability of a JNDI
 * {@link InitialContext} and the ability to lookup specific locations.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/17 14:51
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnJndiCondition.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalOnJndi {

  /**
   * JNDI Locations, one of which must exist. If no locations are specific the condition
   * matches solely based on the presence of an {@link InitialContext}.
   *
   * @return the JNDI locations
   */
  String[] value() default {};

}
