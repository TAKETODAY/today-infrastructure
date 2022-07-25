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

import java.io.Serializable;

/**
 * Canonical Pointcut instance that always matches.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/1 18:24
 * @since 3.0
 */
final class TruePointcut implements Pointcut, Serializable {
  private static final long serialVersionUID = 1L;

  public static final TruePointcut INSTANCE = new TruePointcut();

  /**
   * Enforce Singleton pattern.
   */
  private TruePointcut() { }

  @Override
  public ClassFilter getClassFilter() {
    return ClassFilter.TRUE;
  }

  @Override
  public MethodMatcher getMethodMatcher() {
    return MethodMatcher.TRUE;
  }

  /**
   * Required to support serialization. Replaces with canonical
   * instance on deserialization, protecting Singleton pattern.
   * Alternative to overriding {@code equals()}.
   */
  private Object readResolve() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "Pointcut.TRUE";
  }

}
