/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop.testfixture.advice;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;

@SuppressWarnings("serial")
public class MethodCounter implements Serializable {

  /** Method name --> count, does not understand overloading */
  private HashMap<String, Integer> map = new HashMap<>();

  private int allCount;

  protected void count(Method m) {
    count(m.getName());
  }

  protected void count(String methodName) {
    map.merge(methodName, 1, (n, m) -> n + 1);
    ++allCount;
  }

  public int getCalls(String methodName) {
    return map.getOrDefault(methodName, 0);
  }

  public int getCalls() {
    return allCount;
  }

  /**
   * A bit simplistic: just wants the same class.
   * Doesn't worry about counts.
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object other) {
    return (other != null && other.getClass() == this.getClass());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

}
