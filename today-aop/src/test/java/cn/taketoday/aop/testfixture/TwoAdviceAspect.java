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

package cn.taketoday.aop.testfixture;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class TwoAdviceAspect {
  private int totalCalls;

  @Around("execution(* cn.taketoday.beans.testfixture.beans.ITestBean.age())")
  public int returnCallCount(ProceedingJoinPoint pjp) throws Exception {
    return totalCalls;
  }

  @Before("execution(* cn.taketoday.beans.testfixture.beans.ITestBean.setAge(int)) && args(newAge)")
  public void countSet(int newAge) throws Exception {
    ++totalCalls;
  }
}
