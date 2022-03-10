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

package cn.taketoday.aop.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.testfixture.advice.CountingBeforeAdvice;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for aop namespace.
 *
 * @author Rob Harrop
 * @author Chris Beams
 */
public class AopNamespaceHandlerTests {

  private ApplicationContext context;

  @BeforeEach
  public void setup() {
    this.context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
  }

  protected ITestBean getTestBean() {
    return (ITestBean) this.context.getBean("testBean");
  }

  @Test
  public void testIsProxy() throws Exception {
    ITestBean bean = getTestBean();

    assertThat(AopUtils.isAopProxy(bean)).as("Bean is not a proxy").isTrue();

    // check the advice details
    Advised advised = (Advised) bean;
    Advisor[] advisors = advised.getAdvisors();

    assertThat(advisors.length > 0).as("Advisors should not be empty").isTrue();
  }

  @Test
  public void testAdviceInvokedCorrectly() throws Exception {
    CountingBeforeAdvice getAgeCounter = (CountingBeforeAdvice) this.context.getBean("getAgeCounter");
    CountingBeforeAdvice getNameCounter = (CountingBeforeAdvice) this.context.getBean("getNameCounter");

    ITestBean bean = getTestBean();

    assertThat(getAgeCounter.getCalls("getAge")).as("Incorrect initial getAge count").isEqualTo(0);
    assertThat(getNameCounter.getCalls("getName")).as("Incorrect initial getName count").isEqualTo(0);

    bean.getAge();

    assertThat(getAgeCounter.getCalls("getAge")).as("Incorrect getAge count on getAge counter").isEqualTo(1);
    assertThat(getNameCounter.getCalls("getAge")).as("Incorrect getAge count on getName counter").isEqualTo(0);

    bean.getName();

    assertThat(getNameCounter.getCalls("getName")).as("Incorrect getName count on getName counter").isEqualTo(1);
    assertThat(getAgeCounter.getCalls("getName")).as("Incorrect getName count on getAge counter").isEqualTo(0);
  }

  @Test
  public void testAspectApplied() throws Exception {
    ITestBean bean = getTestBean();

    CountingAspectJAdvice advice = (CountingAspectJAdvice) this.context.getBean("countingAdvice");

    assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(0);
    assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(0);

    bean.setName("Sally");

    assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(1);
    assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(1);

    bean.getName();

    assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(1);
    assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(1);
  }

  @Test
  public void testAspectAppliedForInitializeBeanWithEmptyName() {
    ITestBean bean = (ITestBean) this.context.getAutowireCapableBeanFactory().initializeBean(new TestBean(), "");

    CountingAspectJAdvice advice = (CountingAspectJAdvice) this.context.getBean("countingAdvice");

    assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(0);
    assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(0);

    bean.setName("Sally");

    assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(1);
    assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(1);

    bean.getName();

    assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(1);
    assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(1);
  }

  @Test
  public void testAspectAppliedForInitializeBeanWithNullName() {
    ITestBean bean = (ITestBean) this.context.getAutowireCapableBeanFactory().initializeBean(new TestBean(), null);

    CountingAspectJAdvice advice = (CountingAspectJAdvice) this.context.getBean("countingAdvice");

    assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(0);
    assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(0);

    bean.setName("Sally");

    assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(1);
    assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(1);

    bean.getName();

    assertThat(advice.getBeforeCount()).as("Incorrect before count").isEqualTo(1);
    assertThat(advice.getAfterCount()).as("Incorrect after count").isEqualTo(1);
  }

}

class CountingAspectJAdvice {

  private int beforeCount;

  private int afterCount;

  private int aroundCount;

  public void myBeforeAdvice() throws Throwable {
    this.beforeCount++;
  }

  public void myAfterAdvice() throws Throwable {
    this.afterCount++;
  }

  public void myAroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
    this.aroundCount++;
    pjp.proceed();
  }

  public void myAfterReturningAdvice(int age) {
    this.afterCount++;
  }

  public void myAfterThrowingAdvice(RuntimeException ex) {
    this.afterCount++;
  }

  public void mySetAgeAdvice(int newAge, ITestBean bean) {
    // no-op
  }

  public int getBeforeCount() {
    return this.beforeCount;
  }

  public int getAfterCount() {
    return this.afterCount;
  }

  public int getAroundCount() {
    return this.aroundCount;
  }

}
