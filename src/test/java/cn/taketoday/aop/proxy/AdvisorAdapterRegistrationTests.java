/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.aop.proxy;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.BeforeAdvice;
import cn.taketoday.aop.support.AdvisorAdapterRegistrationManager;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * TestCase for AdvisorAdapterRegistrationManager mechanism.
 *
 * @author Dmitriy Kopylenko
 * @author Chris Beams
 */
public class AdvisorAdapterRegistrationTests {

  @BeforeEach
  @AfterEach
  public void resetGlobalAdvisorAdapterRegistry() {
    DefaultAdvisorAdapterRegistry.reset();
  }

  @Test
  public void testAdvisorAdapterRegistrationManagerNotPresentInContext() {
    GenericApplicationContext beanFactory = new GenericApplicationContext();
    load(beanFactory);

    ITestBean tb = (ITestBean) beanFactory.getBean("testBean");
    // just invoke any method to see if advice fired
    assertThatExceptionOfType(UnknownAdviceTypeException.class)
            .isThrownBy(tb::getName);
    assertThat(getAdviceImpl(tb).getInvocationCounter()).isZero();
  }

  private void load(GenericApplicationContext beanFactory) {
    beanFactory.registerBeanDefinition(new BeanDefinition("testBeanTarget", TestBean.class));
    beanFactory.registerBeanDefinition(new BeanDefinition("simpleBeforeAdvice", SimpleBeforeAdviceImpl.class));
    beanFactory.registerBeanDefinition(new BeanDefinition("testAdvisorAdapter", SimpleBeforeAdviceAdapter.class));

    BeanDefinition testBean = new BeanDefinition("testBean", ProxyFactoryBean.class);

    testBean.addPropertyValue("proxyInterfaces", ITestBean.class);
    testBean.addPropertyValue("interceptorNames", "simpleBeforeAdviceAdvisor,testBeanTarget");
    beanFactory.registerBeanDefinition(testBean);

    BeanDefinition adviceAdvisor = new BeanDefinition("simpleBeforeAdviceAdvisor", DefaultPointcutAdvisor.class);
    adviceAdvisor.addPropertyValue("advice", RuntimeBeanReference.from("simpleBeforeAdvice"));
    beanFactory.registerBeanDefinition(adviceAdvisor);

    beanFactory.refresh();
  }

  @Test
  public void testAdvisorAdapterRegistrationManagerPresentInContext() {
    GenericApplicationContext beanFactory = new GenericApplicationContext();
    beanFactory.registerBean(AdvisorAdapterRegistrationManager.class);
    load(beanFactory);

    ITestBean tb = (ITestBean) beanFactory.getBean("testBean");
    // just invoke any method to see if advice fired
    tb.getName();
    getAdviceImpl(tb).getInvocationCounter();
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
