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

package cn.taketoday.aop.aspectj.generic;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import cn.taketoday.beans.testfixture.beans.Employee;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests ensuring that after-returning advice for generic parameters bound to
 * the advice and the return type follow AspectJ semantics.
 *
 * <p>See SPR-3628 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class AfterReturningGenericTypeMatchingTests {

  private GenericReturnTypeVariationClass testBean;

  private CounterAspect counterAspect;

  @BeforeEach
  public void setup() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

    counterAspect = (CounterAspect) ctx.getBean("counterAspect");
    counterAspect.reset();

    testBean = (GenericReturnTypeVariationClass) ctx.getBean("testBean");
  }

  @Test
  public void testReturnTypeExactMatching() {
    testBean.getStrings();
    assertThat(counterAspect.getStringsInvocationsCount).isEqualTo(1);
    assertThat(counterAspect.getIntegersInvocationsCount).isEqualTo(0);

    counterAspect.reset();

    testBean.getIntegers();
    assertThat(counterAspect.getStringsInvocationsCount).isEqualTo(0);
    assertThat(counterAspect.getIntegersInvocationsCount).isEqualTo(1);
  }

  @Test
  public void testReturnTypeRawMatching() {
    testBean.getStrings();
    assertThat(counterAspect.getRawsInvocationsCount).isEqualTo(1);

    counterAspect.reset();

    testBean.getIntegers();
    assertThat(counterAspect.getRawsInvocationsCount).isEqualTo(1);
  }

  @Test
  public void testReturnTypeUpperBoundMatching() {
    testBean.getIntegers();
    assertThat(counterAspect.getNumbersInvocationsCount).isEqualTo(1);
  }

  @Test
  public void testReturnTypeLowerBoundMatching() {
    testBean.getTestBeans();
    assertThat(counterAspect.getTestBeanInvocationsCount).isEqualTo(1);

    counterAspect.reset();

    testBean.getEmployees();
    assertThat(counterAspect.getTestBeanInvocationsCount).isEqualTo(0);
  }

}

class GenericReturnTypeVariationClass {

  public Collection<String> getStrings() {
    return new ArrayList<>();
  }

  public Collection<Integer> getIntegers() {
    return new ArrayList<>();
  }

  public Collection<TestBean> getTestBeans() {
    return new ArrayList<>();
  }

  public Collection<Employee> getEmployees() {
    return new ArrayList<>();
  }
}

@Aspect
class CounterAspect {

  int getRawsInvocationsCount;

  int getStringsInvocationsCount;

  int getIntegersInvocationsCount;

  int getNumbersInvocationsCount;

  int getTestBeanInvocationsCount;

  @Pointcut("execution(* cn.taketoday.aop.aspectj.generic.GenericReturnTypeVariationClass.*(..))")
  public void anyTestMethod() {
  }

  @AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
  public void incrementGetRawsInvocationsCount(Collection<?> ret) {
    getRawsInvocationsCount++;
  }

  @AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
  public void incrementGetStringsInvocationsCount(Collection<String> ret) {
    getStringsInvocationsCount++;
  }

  @AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
  public void incrementGetIntegersInvocationsCount(Collection<Integer> ret) {
    getIntegersInvocationsCount++;
  }

  @AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
  public void incrementGetNumbersInvocationsCount(Collection<? extends Number> ret) {
    getNumbersInvocationsCount++;
  }

  @AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
  public void incrementTestBeanInvocationsCount(Collection<? super TestBean> ret) {
    getTestBeanInvocationsCount++;
  }

  public void reset() {
    getRawsInvocationsCount = 0;
    getStringsInvocationsCount = 0;
    getIntegersInvocationsCount = 0;
    getNumbersInvocationsCount = 0;
    getTestBeanInvocationsCount = 0;
  }
}

