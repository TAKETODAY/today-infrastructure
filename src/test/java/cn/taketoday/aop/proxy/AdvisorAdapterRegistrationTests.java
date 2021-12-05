/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.StandardBeanFactory;
import cn.taketoday.beans.factory.support.ITestBean;
import cn.taketoday.beans.factory.support.TestBean;

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
    StandardBeanFactory beanFactory = new StandardBeanFactory();

    load(beanFactory);

    ITestBean tb = (ITestBean) beanFactory.getBean("testBean");
    // just invoke any method to see if advice fired
    assertThatExceptionOfType(UnknownAdviceTypeException.class)
            .isThrownBy(tb::getName);
    assertThat(getAdviceImpl(tb).getInvocationCounter()).isZero();
  }

  private void load(StandardBeanFactory beanFactory) {
    beanFactory.registerBeanDefinition(new BeanDefinition("testBeanTarget", TestBean.class));
    beanFactory.registerBeanDefinition(new BeanDefinition("simpleBeforeAdvice", SimpleBeforeAdviceImpl.class));
    beanFactory.registerBeanDefinition(new BeanDefinition("simpleBeforeAdviceAdvisor", DefaultPointcutAdvisor.class));
    beanFactory.registerBeanDefinition(new BeanDefinition("testAdvisorAdapter", SimpleBeforeAdviceAdapter.class));

    BeanDefinition testBean = new BeanDefinition("testBean", ProxyFactoryBean.class);

    testBean.addPropertyValue("proxyInterfaces", ITestBean.class);
    testBean.addPropertyValue("interceptorNames", "simpleBeforeAdviceAdvisor,testBeanTarget");
    beanFactory.registerBeanDefinition(testBean);
  }

  @Test
  public void testAdvisorAdapterRegistrationManagerPresentInContext() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
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
