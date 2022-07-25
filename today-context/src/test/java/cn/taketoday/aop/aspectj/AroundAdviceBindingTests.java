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

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class AroundAdviceBindingTests {

  private AroundAdviceBindingTestAspect.AroundAdviceBindingCollaborator mockCollaborator;

  private ITestBean testBeanProxy;

  private TestBean testBeanTarget;

  protected ApplicationContext ctx;

  @BeforeEach
  public void onSetUp() throws Exception {
    ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

    AroundAdviceBindingTestAspect aroundAdviceAspect = ((AroundAdviceBindingTestAspect) ctx.getBean("testAspect"));

    ITestBean injectedTestBean = (ITestBean) ctx.getBean("testBean");
    assertThat(AopUtils.isAopProxy(injectedTestBean)).isTrue();

    this.testBeanProxy = injectedTestBean;
    // we need the real target too, not just the proxy...

    this.testBeanTarget = (TestBean) ((Advised) testBeanProxy).getTargetSource().getTarget();

    mockCollaborator = mock(AroundAdviceBindingTestAspect.AroundAdviceBindingCollaborator.class);
    aroundAdviceAspect.setCollaborator(mockCollaborator);
  }

  @Test
  public void testOneIntArg() {
    testBeanProxy.setAge(5);
    verify(mockCollaborator).oneIntArg(5);
  }

  @Test
  public void testOneObjectArgBoundToTarget() {
    testBeanProxy.getAge();
    verify(mockCollaborator).oneObjectArg(this.testBeanTarget);
  }

  @Test
  public void testOneIntAndOneObjectArgs() {
    testBeanProxy.setAge(5);
    verify(mockCollaborator).oneIntAndOneObject(5, this.testBeanProxy);
  }

  @Test
  public void testJustJoinPoint() {
    testBeanProxy.getAge();
    verify(mockCollaborator).justJoinPoint("getAge");
  }

}

class AroundAdviceBindingTestAspect {

  private AroundAdviceBindingCollaborator collaborator = null;

  public void setCollaborator(AroundAdviceBindingCollaborator aCollaborator) {
    this.collaborator = aCollaborator;
  }

  // "advice" methods
  public void oneIntArg(ProceedingJoinPoint pjp, int age) throws Throwable {
    this.collaborator.oneIntArg(age);
    pjp.proceed();
  }

  public int oneObjectArg(ProceedingJoinPoint pjp, Object bean) throws Throwable {
    this.collaborator.oneObjectArg(bean);
    return ((Integer) pjp.proceed()).intValue();
  }

  public void oneIntAndOneObject(ProceedingJoinPoint pjp, int x, Object o) throws Throwable {
    this.collaborator.oneIntAndOneObject(x, o);
    pjp.proceed();
  }

  public int justJoinPoint(ProceedingJoinPoint pjp) throws Throwable {
    this.collaborator.justJoinPoint(pjp.getSignature().getName());
    return ((Integer) pjp.proceed()).intValue();
  }

  /**
   * Collaborator interface that makes it easy to test this aspect
   * is working as expected through mocking.
   */
  public interface AroundAdviceBindingCollaborator {

    void oneIntArg(int x);

    void oneObjectArg(Object o);

    void oneIntAndOneObject(int x, Object o);

    void justJoinPoint(String s);
  }

}
