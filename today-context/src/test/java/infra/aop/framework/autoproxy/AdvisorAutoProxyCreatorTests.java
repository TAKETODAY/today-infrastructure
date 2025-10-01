/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aop.framework.autoproxy;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.aop.Advisor;
import infra.aop.framework.Advised;
import infra.aop.framework.autoproxy.target.AbstractBeanFactoryTargetSourceCreator;
import infra.aop.support.AopUtils;
import infra.aop.target.AbstractBeanFactoryTargetSource;
import infra.aop.target.CommonsPool2TargetSource;
import infra.aop.target.LazyInitTargetSource;
import infra.aop.target.PrototypeTargetSource;
import infra.aop.target.ThreadLocalTargetSource;
import infra.aop.testfixture.advice.CountingBeforeAdvice;
import infra.aop.testfixture.interceptor.NopInterceptor;
import infra.beans.factory.BeanFactory;
import infra.beans.testfixture.beans.CountingTestBean;
import infra.beans.testfixture.beans.ITestBean;
import infra.context.support.ClassPathXmlApplicationContext;
import test.mixin.Lockable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for auto proxy creation by advisor recognition.
 *
 * @author Rod Johnson
 * @author Dave Syer
 * @author Chris Beams
 * @see infra.aop.framework.autoproxy.AdvisorAutoProxyCreatorIntegrationTests
 */
@SuppressWarnings("resource")
public class AdvisorAutoProxyCreatorTests {

  private static final Class<?> CLASS = AdvisorAutoProxyCreatorTests.class;
  private static final String CLASSNAME = CLASS.getSimpleName();

  private static final String DEFAULT_CONTEXT = CLASSNAME + "-context.xml";
  private static final String COMMON_INTERCEPTORS_CONTEXT = CLASSNAME + "-common-interceptors.xml";
  private static final String CUSTOM_TARGETSOURCE_CONTEXT = CLASSNAME + "-custom-targetsource.xml";
  private static final String QUICK_TARGETSOURCE_CONTEXT = CLASSNAME + "-quick-targetsource.xml";
  private static final String OPTIMIZED_CONTEXT = CLASSNAME + "-optimized.xml";

  /**
   * Return a bean factory with attributes and EnterpriseServices configured.
   */
  protected BeanFactory getBeanFactory() throws IOException {
    return new ClassPathXmlApplicationContext(DEFAULT_CONTEXT, CLASS);
  }

  /**
   * Check that we can provide a common interceptor that will
   * appear in the chain before "specific" interceptors,
   * which are sourced from matching advisors
   */
  @Test
  public void testCommonInterceptorAndAdvisor() throws Exception {
    BeanFactory bf = new ClassPathXmlApplicationContext(COMMON_INTERCEPTORS_CONTEXT, CLASS);
    ITestBean test1 = (ITestBean) bf.getBean("test1");
    assertThat(AopUtils.isAopProxy(test1)).isTrue();

    Lockable lockable1 = (Lockable) test1;
    NopInterceptor nop1 = (NopInterceptor) bf.getBean("nopInterceptor");
    NopInterceptor nop2 = (NopInterceptor) bf.getBean("pointcutAdvisor", Advisor.class).getAdvice();

    ITestBean test2 = (ITestBean) bf.getBean("test2");
    Lockable lockable2 = (Lockable) test2;

    // Locking should be independent; nop is shared
    assertThat(lockable1.locked()).isFalse();
    assertThat(lockable2.locked()).isFalse();
    // equals 2 calls on shared nop, because it's first and sees calls
    // against the Lockable interface introduced by the specific advisor
    assertThat(nop1.getCount()).isEqualTo(2);
    assertThat(nop2.getCount()).isEqualTo(0);
    lockable1.lock();
    assertThat(lockable1.locked()).isTrue();
    assertThat(lockable2.locked()).isFalse();
    assertThat(nop1.getCount()).isEqualTo(5);
    assertThat(nop2.getCount()).isEqualTo(0);

    PackageVisibleMethod packageVisibleMethod = (PackageVisibleMethod) bf.getBean("packageVisibleMethod");
    assertThat(nop1.getCount()).isEqualTo(5);
    assertThat(nop2.getCount()).isEqualTo(0);
    packageVisibleMethod.doSomething();
    assertThat(nop1.getCount()).isEqualTo(6);
    assertThat(nop2.getCount()).isEqualTo(1);
    boolean condition = packageVisibleMethod instanceof Lockable;
    assertThat(condition).isTrue();
    Lockable lockable3 = (Lockable) packageVisibleMethod;
    lockable3.lock();
    assertThat(lockable3.locked()).isTrue();
    lockable3.unlock();
    assertThat(lockable3.locked()).isFalse();
  }

  /**
   * We have custom TargetSourceCreators but there's no match, and
   * hence no proxying, for this bean
   */
  @Test
  public void testCustomTargetSourceNoMatch() throws Exception {
    BeanFactory bf = new ClassPathXmlApplicationContext(CUSTOM_TARGETSOURCE_CONTEXT, CLASS);
    ITestBean test = (ITestBean) bf.getBean("test");
    assertThat(AopUtils.isAopProxy(test)).isFalse();
    assertThat(test.getName()).isEqualTo("Rod");
    assertThat(test.getSpouse().getName()).isEqualTo("Kerry");
  }

  @Test
  public void testCustomPrototypeTargetSource() throws Exception {
    CountingTestBean.count = 0;
    BeanFactory bf = new ClassPathXmlApplicationContext(CUSTOM_TARGETSOURCE_CONTEXT, CLASS);
    ITestBean test = (ITestBean) bf.getBean("prototypeTest");
    assertThat(AopUtils.isAopProxy(test)).isTrue();
    Advised advised = (Advised) test;
    boolean condition = advised.getTargetSource() instanceof PrototypeTargetSource;
    assertThat(condition).isTrue();
    assertThat(test.getName()).isEqualTo("Rod");
    // Check that references survived prototype creation
    assertThat(test.getSpouse().getName()).isEqualTo("Kerry");
    assertThat(CountingTestBean.count).as("Only 2 CountingTestBeans instantiated").isEqualTo(2);
    CountingTestBean.count = 0;
  }

  @Test
  public void testLazyInitTargetSource() throws Exception {
    CountingTestBean.count = 0;
    BeanFactory bf = new ClassPathXmlApplicationContext(CUSTOM_TARGETSOURCE_CONTEXT, CLASS);
    ITestBean test = (ITestBean) bf.getBean("lazyInitTest");
    assertThat(AopUtils.isAopProxy(test)).isTrue();
    Advised advised = (Advised) test;
    boolean condition = advised.getTargetSource() instanceof LazyInitTargetSource;
    assertThat(condition).isTrue();
    assertThat(CountingTestBean.count).as("No CountingTestBean instantiated yet").isEqualTo(0);
    assertThat(test.getName()).isEqualTo("Rod");
    assertThat(test.getSpouse().getName()).isEqualTo("Kerry");
    assertThat(CountingTestBean.count).as("Only 1 CountingTestBean instantiated").isEqualTo(1);
    CountingTestBean.count = 0;
  }

  @Test
  public void testQuickTargetSourceCreator() throws Exception {
    ClassPathXmlApplicationContext bf =
            new ClassPathXmlApplicationContext(QUICK_TARGETSOURCE_CONTEXT, CLASS);
    ITestBean test = (ITestBean) bf.getBean("test");
    assertThat(AopUtils.isAopProxy(test)).isFalse();
    assertThat(test.getName()).isEqualTo("Rod");
    // Check that references survived pooling
    assertThat(test.getSpouse().getName()).isEqualTo("Kerry");

    // Now test the pooled one
    test = (ITestBean) bf.getBean(":test");
    assertThat(AopUtils.isAopProxy(test)).isTrue();
    Advised advised = (Advised) test;
    boolean condition2 = advised.getTargetSource() instanceof CommonsPool2TargetSource;
    assertThat(condition2).isTrue();
    assertThat(test.getName()).isEqualTo("Rod");
    // Check that references survived pooling
    assertThat(test.getSpouse().getName()).isEqualTo("Kerry");

    // Now test the ThreadLocal one
    test = (ITestBean) bf.getBean("%test");
    assertThat(AopUtils.isAopProxy(test)).isTrue();
    advised = (Advised) test;
    boolean condition1 = advised.getTargetSource() instanceof ThreadLocalTargetSource;
    assertThat(condition1).isTrue();
    assertThat(test.getName()).isEqualTo("Rod");
    // Check that references survived pooling
    assertThat(test.getSpouse().getName()).isEqualTo("Kerry");

    // Now test the Prototype TargetSource
    test = (ITestBean) bf.getBean("!test");
    assertThat(AopUtils.isAopProxy(test)).isTrue();
    advised = (Advised) test;
    boolean condition = advised.getTargetSource() instanceof PrototypeTargetSource;
    assertThat(condition).isTrue();
    assertThat(test.getName()).isEqualTo("Rod");
    // Check that references survived pooling
    assertThat(test.getSpouse().getName()).isEqualTo("Kerry");

    ITestBean test2 = (ITestBean) bf.getBean("!test");
    assertThat(test == test2).as("Prototypes cannot be the same object").isFalse();
    assertThat(test2.getName()).isEqualTo("Rod");
    assertThat(test2.getSpouse().getName()).isEqualTo("Kerry");
    bf.close();
  }

  @Test
  public void testWithOptimizedProxy() throws Exception {
    BeanFactory beanFactory = new ClassPathXmlApplicationContext(OPTIMIZED_CONTEXT, CLASS);

    ITestBean testBean = (ITestBean) beanFactory.getBean("optimizedTestBean");
    assertThat(AopUtils.isAopProxy(testBean)).isTrue();

    CountingBeforeAdvice beforeAdvice = (CountingBeforeAdvice) beanFactory.getBean("countingAdvice");

    testBean.setAge(23);
    testBean.getAge();

    assertThat(beforeAdvice.getCalls()).as("Incorrect number of calls to proxy").isEqualTo(2);
  }

}

class SelectivePrototypeTargetSourceCreator extends AbstractBeanFactoryTargetSourceCreator {

  @Nullable
  @Override
  protected AbstractBeanFactoryTargetSource createBeanFactoryTargetSource(Class<?> beanClass, String beanName) {
    if (!beanName.startsWith("prototype")) {
      return null;
    }
    return new PrototypeTargetSource();
  }
}

