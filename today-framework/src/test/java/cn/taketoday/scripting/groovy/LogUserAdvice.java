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
package cn.taketoday.scripting.groovy;

import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.aop.MethodBeforeAdvice;
import cn.taketoday.aop.ThrowsAdvice;

public class LogUserAdvice implements MethodBeforeAdvice, ThrowsAdvice {

  private int countBefore = 0;

  private int countThrows = 0;

  @Override
  public void before(MethodInvocation invocation) throws Throwable {
    countBefore++;
    // System.out.println("Method:" + method.getName());
  }

  public Object afterThrowing(Throwable ex) throws Throwable {
    countThrows++;
    // System.out.println("***********************************************************************************");
    // System.out.println("Exception caught:");
    // System.out.println("***********************************************************************************");
    // e.printStackTrace();
    throw ex;
  }

  public int getCountBefore() {
    return countBefore;
  }

  public int getCountThrows() {
    return countThrows;
  }

  public void reset() {
    countThrows = 0;
    countBefore = 0;
  }

}
