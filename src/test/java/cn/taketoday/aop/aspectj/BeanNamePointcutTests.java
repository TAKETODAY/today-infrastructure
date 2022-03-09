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

package cn.taketoday.aop.aspectj;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.aop.MethodBeforeAdvice;
import cn.taketoday.aop.framework.Advised;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for correct application of the bean() PCD for XML-based AspectJ aspects.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class BeanNamePointcutTests {

  private ITestBean testBean1;
  private ITestBean testBean2;
  private ITestBean testBeanContainingNestedBean;
  private Map<?, ?> testFactoryBean1;
  private Map<?, ?> testFactoryBean2;
  private Counter counterAspect;

  private ITestBean interceptThis;
  private ITestBean dontInterceptThis;
  private TestInterceptor testInterceptor;

  private ClassPathXmlApplicationContext ctx;

  @BeforeEach
  public void setup() {
    ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
    testBean1 = (ITestBean) ctx.getBean("testBean1");
    testBean2 = (ITestBean) ctx.getBean("testBean2");
    testBeanContainingNestedBean = (ITestBean) ctx.getBean("testBeanContainingNestedBean");
    testFactoryBean1 = (Map<?, ?>) ctx.getBean("testFactoryBean1");
    testFactoryBean2 = (Map<?, ?>) ctx.getBean("testFactoryBean2");
    counterAspect = (Counter) ctx.getBean("counterAspect");
    interceptThis = (ITestBean) ctx.getBean("interceptThis");
    dontInterceptThis = (ITestBean) ctx.getBean("dontInterceptThis");
    testInterceptor = (TestInterceptor) ctx.getBean("testInterceptor");

    counterAspect.reset();
  }

  // We don't need to test all combination of pointcuts due to BeanNamePointcutMatchingTests

  @Test
  public void testMatchingBeanName() {
    boolean condition = this.testBean1 instanceof Advised;
    assertThat(condition).as("Matching bean must be advised (proxied)").isTrue();
    // Call two methods to test for SPR-3953-like condition
    this.testBean1.setAge(20);
    this.testBean1.setName("");
    assertThat(this.counterAspect.getCount()).as("Advice not executed: must have been").isEqualTo(2);
  }

  @Test
  public void testNonMatchingBeanName() {
    boolean condition = this.testBean2 instanceof Advised;
    assertThat(condition).as("Non-matching bean must *not* be advised (proxied)").isFalse();
    this.testBean2.setAge(20);
    assertThat(this.counterAspect.getCount()).as("Advice must *not* have been executed").isEqualTo(0);
  }

  @Test
  public void testNonMatchingNestedBeanName() {
    boolean condition = this.testBeanContainingNestedBean.getDoctor() instanceof Advised;
    assertThat(condition).as("Non-matching bean must *not* be advised (proxied)").isFalse();
  }

  @Test
  public void testMatchingFactoryBeanObject() {
    boolean condition1 = this.testFactoryBean1 instanceof Advised;
    assertThat(condition1).as("Matching bean must be advised (proxied)").isTrue();
    assertThat(this.testFactoryBean1.get("myKey")).isEqualTo("myValue");
    assertThat(this.testFactoryBean1.get("myKey")).isEqualTo("myValue");
    assertThat(this.counterAspect.getCount()).as("Advice not executed: must have been").isEqualTo(2);
    FactoryBean<?> fb = (FactoryBean<?>) ctx.getBean("&testFactoryBean1");
    boolean condition = !(fb instanceof Advised);
    assertThat(condition).as("FactoryBean itself must *not* be advised").isTrue();
  }

  @Test
  public void testMatchingFactoryBeanItself() {
    boolean condition1 = !(this.testFactoryBean2 instanceof Advised);
    assertThat(condition1).as("Matching bean must *not* be advised (proxied)").isTrue();
    FactoryBean<?> fb = (FactoryBean<?>) ctx.getBean("&testFactoryBean2");
    boolean condition = fb instanceof Advised;
    assertThat(condition).as("FactoryBean itself must be advised").isTrue();
    assertThat(Map.class.isAssignableFrom(fb.getObjectType())).isTrue();
    assertThat(Map.class.isAssignableFrom(fb.getObjectType())).isTrue();
    assertThat(this.counterAspect.getCount()).as("Advice not executed: must have been").isEqualTo(2);
  }

  @Test
  public void testPointcutAdvisorCombination() {
    boolean condition = this.interceptThis instanceof Advised;
    assertThat(condition).as("Matching bean must be advised (proxied)").isTrue();
    boolean condition1 = this.dontInterceptThis instanceof Advised;
    assertThat(condition1).as("Non-matching bean must *not* be advised (proxied)").isFalse();
    interceptThis.setAge(20);
    assertThat(testInterceptor.interceptionCount).isEqualTo(1);
    dontInterceptThis.setAge(20);
    assertThat(testInterceptor.interceptionCount).isEqualTo(1);
  }

  public static class TestInterceptor implements MethodBeforeAdvice {

    private int interceptionCount;

    @Override
    public void before(MethodInvocation invocation) throws Throwable {
      interceptionCount++;
    }
  }

}
