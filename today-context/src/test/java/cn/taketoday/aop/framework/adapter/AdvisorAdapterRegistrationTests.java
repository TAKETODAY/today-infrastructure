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

package cn.taketoday.aop.framework.adapter;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.BeforeAdvice;
import cn.taketoday.aop.framework.Advised;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * TestCase for AdvisorAdapterRegistrationManager mechanism.
 *
 * @author Dmitriy Kopylenko
 * @author Chris Beams
 */
public class AdvisorAdapterRegistrationTests {

//  @BeforeEach
//  @AfterEach
  public void resetGlobalAdvisorAdapterRegistry() {
    DefaultAdvisorAdapterRegistry.reset();
  }

  @Test
  @Disabled
  public void testAdvisorAdapterRegistrationManagerNotPresentInContext() {
    try (var ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-without-bpp.xml", getClass())) {
      ITestBean tb = (ITestBean) ctx.getBean("testBean");
      // just invoke any method to see if advice fired
      assertThatExceptionOfType(UnknownAdviceTypeException.class)
              .isThrownBy(tb::getName);
      assertThat(getAdviceImpl(tb).getInvocationCounter()).isZero();
    }
  }

  @Test
  public void testAdvisorAdapterRegistrationManagerPresentInContext() {
    try (var ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-with-bpp.xml", getClass())) {
      ITestBean tb = (ITestBean) ctx.getBean("testBean");

      // just invoke any method to see if advice fired
      tb.getName();
      getAdviceImpl(tb).getInvocationCounter();
    }
  }

  private SimpleBeforeAdviceImpl getAdviceImpl(ITestBean tb) {
    Advised advised = (Advised) tb;
    Advisor advisor = advised.getAdvisors()[0];
    return (SimpleBeforeAdviceImpl) advisor.getAdvice();
  }

}

interface SimpleBeforeAdvice extends BeforeAdvice {

  void before() throws Throwable;

}

@SuppressWarnings("serial")
class SimpleBeforeAdviceAdapter implements AdvisorAdapter, Serializable {

  @Override
  public boolean supportsAdvice(Advice advice) {
    return (advice instanceof SimpleBeforeAdvice);
  }

  @Override
  public MethodInterceptor getInterceptor(Advisor advisor) {
    SimpleBeforeAdvice advice = (SimpleBeforeAdvice) advisor.getAdvice();
    return new SimpleBeforeAdviceInterceptor(advice);
  }

}

class SimpleBeforeAdviceImpl implements SimpleBeforeAdvice {

  private int invocationCounter;

  @Override
  public void before() throws Throwable {
    ++invocationCounter;
  }

  public int getInvocationCounter() {
    return invocationCounter;
  }

}

final class SimpleBeforeAdviceInterceptor implements MethodInterceptor {

  private SimpleBeforeAdvice advice;

  public SimpleBeforeAdviceInterceptor(SimpleBeforeAdvice advice) {
    this.advice = advice;
  }

  @Override
  public Object invoke(MethodInvocation mi) throws Throwable {
    advice.before();
    return mi.proceed();
  }
}
