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

package infra.aop;

import java.io.Serial;
import java.io.Serializable;

/**
 * Canonical Pointcut instance that always matches.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/1 18:24
 * @since 3.0
 */
final class TruePointcut implements Pointcut, Serializable {
  @Serial
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
  @Serial
  private Object readResolve() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "Pointcut.TRUE";
  }

}
