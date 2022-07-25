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

package cn.taketoday.aop;

import cn.taketoday.aop.support.ClassFilters;
import cn.taketoday.aop.support.ComposablePointcut;
import cn.taketoday.aop.support.MethodMatchers;
import cn.taketoday.aop.support.Pointcuts;

/**
 * Core  pointcut abstraction.
 *
 * <p>A pointcut is composed of a {@link ClassFilter} and a {@link MethodMatcher}.
 * Both these basic terms and a Pointcut itself can be combined to build up combinations
 * (e.g. through {@link ComposablePointcut}).
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/1 18:12
 * @see ClassFilter
 * @see MethodMatcher
 * @see Pointcuts
 * @see ClassFilters
 * @see MethodMatchers
 * @since 3.0
 */
public interface Pointcut {

  /**
   * Return the ClassFilter for this pointcut.
   *
   * @return the ClassFilter (never {@code null})
   */
  ClassFilter getClassFilter();

  /**
   * Return the MethodMatcher for this pointcut.
   *
   * @return the MethodMatcher (never {@code null})
   */
  MethodMatcher getMethodMatcher();

  /**
   * Canonical Pointcut instance that always matches.
   */
  Pointcut TRUE = TruePointcut.INSTANCE;

}
