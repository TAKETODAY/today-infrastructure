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

package infra.aop.aspectj;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.aop.aspectj.annotation.AspectJProxyFactory;
import infra.aop.framework.Advised;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for correct application of the bean() PCD for &#64;AspectJ-based aspects.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class BeanNamePointcutAtAspectTests {

  private ITestBean testBean1;

  private ITestBean testBean3;

  private CounterAspect counterAspect;

  @BeforeEach
  public void setup() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext("BeanNamePointcutAtAspectTests.xml", getClass());

    counterAspect = (CounterAspect) ctx.getBean("counterAspect");
    testBean1 = (ITestBean) ctx.getBean("testBean1");
    testBean3 = (ITestBean) ctx.getBean("testBean3");
  }

  @Test
  public void testMatchingBeanName() {
    boolean condition = testBean1 instanceof Advised;
    assertThat(condition).as("Expected a proxy").isTrue();

    testBean1.setAge(20);
    testBean1.setName("");
    assertThat(counterAspect.count).isEqualTo(2);
  }

  @Test
  public void testNonMatchingBeanName() {
    boolean condition = testBean3 instanceof Advised;
    assertThat(condition).as("Didn't expect a proxy").isFalse();

    testBean3.setAge(20);
    assertThat(counterAspect.count).isEqualTo(0);
  }

  @Test
  public void testProgrammaticProxyCreation() {
    ITestBean testBean = new TestBean();

    AspectJProxyFactory factory = new AspectJProxyFactory();
    factory.setTarget(testBean);

    CounterAspect myCounterAspect = new CounterAspect();
    factory.addAspect(myCounterAspect);

    ITestBean proxyTestBean = factory.getProxy();

    boolean condition = proxyTestBean instanceof Advised;
    assertThat(condition).as("Expected a proxy").isTrue();
    proxyTestBean.setAge(20);
    assertThat(myCounterAspect.count).as("Programmatically created proxy shouldn't match bean()").isEqualTo(0);
  }

}

@Aspect
class CounterAspect {

  int count;

  @Before("execution(* set*(..)) && bean(testBean1)")
  public void increment1ForAnonymousPointcut() {
    count++;
  }

}
