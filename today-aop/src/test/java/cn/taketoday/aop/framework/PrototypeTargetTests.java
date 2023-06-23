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

package cn.taketoday.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.core.io.Resource;

import static cn.taketoday.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 03.09.2004
 */
public class PrototypeTargetTests {

  private static final Resource CONTEXT = qualifiedResource(PrototypeTargetTests.class, "context.xml");

  @Test
  public void testPrototypeProxyWithPrototypeTarget() {
    TestBeanImpl.constructionCount = 0;
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(CONTEXT);
    for (int i = 0; i < 10; i++) {
      TestBean tb = (TestBean) bf.getBean("testBeanPrototype");
      tb.doSomething();
    }
    TestInterceptor interceptor = (TestInterceptor) bf.getBean("testInterceptor");
    assertThat(TestBeanImpl.constructionCount).isEqualTo(10);
    assertThat(interceptor.invocationCount).isEqualTo(10);
  }

  @Test
  public void testSingletonProxyWithPrototypeTarget() {
    TestBeanImpl.constructionCount = 0;
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(CONTEXT);
    for (int i = 0; i < 10; i++) {
      TestBean tb = (TestBean) bf.getBean("testBeanSingleton");
      tb.doSomething();
    }
    TestInterceptor interceptor = (TestInterceptor) bf.getBean("testInterceptor");
    assertThat(TestBeanImpl.constructionCount).isEqualTo(1);
    assertThat(interceptor.invocationCount).isEqualTo(10);
  }

  public interface TestBean {

    void doSomething();
  }

  public static class TestBeanImpl implements TestBean {

    private static int constructionCount = 0;

    public TestBeanImpl() {
      constructionCount++;
    }

    @Override
    public void doSomething() {
    }
  }

  public static class TestInterceptor implements MethodInterceptor {

    private int invocationCount = 0;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
      invocationCount++;
      return methodInvocation.proceed();
    }
  }

}
