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

package cn.taketoday.aop.framework.autoproxy;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.testfixture.advice.CountingBeforeAdvice;
import cn.taketoday.aop.testfixture.interceptor.NopInterceptor;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.core.testfixture.TimeStamped;
import test.mixin.Lockable;
import test.mixin.LockedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Chris Beams
 */
class BeanNameAutoProxyCreatorTests {

  // Note that we need an ApplicationContext, not just a BeanFactory,
  // for post-processing and hence auto-proxying to work.
  private final BeanFactory beanFactory = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

  @Test
  void noProxy() {
    TestBean tb = (TestBean) beanFactory.getBean("noproxy");
    assertThat(AopUtils.isAopProxy(tb)).isFalse();
    assertThat(tb.getName()).isEqualTo("noproxy");
  }

  @Test
  void proxyWithExactNameMatch() {
    ITestBean tb = (ITestBean) beanFactory.getBean("onlyJdk");
    jdkAssertions(tb, 1);
    assertThat(tb.getName()).isEqualTo("onlyJdk");
  }

  @Test
  void proxyWithDoubleProxying() {
    ITestBean tb = (ITestBean) beanFactory.getBean("doubleJdk");
    jdkAssertions(tb, 2);
    assertThat(tb.getName()).isEqualTo("doubleJdk");
  }

  @Test
  void jdkIntroduction() {
    ITestBean tb = (ITestBean) beanFactory.getBean("introductionUsingJdk");
    NopInterceptor nop = (NopInterceptor) beanFactory.getBean("introductionNopInterceptor");
    assertThat(nop.getCount()).isEqualTo(0);
    assertThat(AopUtils.isJdkDynamicProxy(tb)).isTrue();
    int age = 5;
    tb.setAge(age);
    assertThat(tb.getAge()).isEqualTo(age);
    boolean condition = tb instanceof TimeStamped;
    assertThat(condition).as("Introduction was made").isTrue();
    assertThat(((TimeStamped) tb).getTimeStamp()).isEqualTo(0);
    assertThat(nop.getCount()).isEqualTo(3);
    assertThat(tb.getName()).isEqualTo("introductionUsingJdk");

    ITestBean tb2 = (ITestBean) beanFactory.getBean("second-introductionUsingJdk");

    // Check two per-instance mixins were distinct
    Lockable lockable1 = (Lockable) tb;
    Lockable lockable2 = (Lockable) tb2;
    assertThat(lockable1.locked()).isFalse();
    assertThat(lockable2.locked()).isFalse();
    tb.setAge(65);
    assertThat(tb.getAge()).isEqualTo(65);
    lockable1.lock();
    assertThat(lockable1.locked()).isTrue();
    // Shouldn't affect second
    assertThat(lockable2.locked()).isFalse();
    // Can still mod second object
    tb2.setAge(12);
    // But can't mod first
    assertThatExceptionOfType(LockedException.class).as("mixin should have locked this object").isThrownBy(() ->
            tb.setAge(6));
  }

  @Test
  void jdkIntroductionAppliesToCreatedObjectsNotFactoryBean() {
    ITestBean tb = (ITestBean) beanFactory.getBean("factory-introductionUsingJdk");
    NopInterceptor nop = (NopInterceptor) beanFactory.getBean("introductionNopInterceptor");
    assertThat(nop.getCount()).as("NOP should not have done any work yet").isEqualTo(0);
    assertThat(AopUtils.isJdkDynamicProxy(tb)).isTrue();
    int age = 5;
    tb.setAge(age);
    assertThat(tb.getAge()).isEqualTo(age);
    assertThat(tb).as("Introduction was made").isInstanceOf(TimeStamped.class);
    assertThat(((TimeStamped) tb).getTimeStamp()).isEqualTo(0);
    assertThat(nop.getCount()).isEqualTo(3);

    ITestBean tb2 = (ITestBean) beanFactory.getBean("second-introductionUsingJdk");

    // Check two per-instance mixins were distinct
    Lockable lockable1 = (Lockable) tb;
    Lockable lockable2 = (Lockable) tb2;
    assertThat(lockable1.locked()).isFalse();
    assertThat(lockable2.locked()).isFalse();
    tb.setAge(65);
    assertThat(tb.getAge()).isEqualTo(65);
    lockable1.lock();
    assertThat(lockable1.locked()).isTrue();
    // Shouldn't affect second
    assertThat(lockable2.locked()).isFalse();
    // Can still mod second object
    tb2.setAge(12);
    // But can't mod first
    assertThatExceptionOfType(LockedException.class).as("mixin should have locked this object").isThrownBy(() ->
            tb.setAge(6));
  }

  @Test
  void proxyWithWildcardMatch() {
    ITestBean tb = (ITestBean) beanFactory.getBean("jdk1");
    jdkAssertions(tb, 1);
    assertThat(tb.getName()).isEqualTo("jdk1");
  }

  @Test
  void cglibProxyWithWildcardMatch() {
    TestBean tb = (TestBean) beanFactory.getBean("cglib1");
    cglibAssertions(tb);
    assertThat(tb.getName()).isEqualTo("cglib1");
  }

  @Test
  void withFrozenProxy() {
    ITestBean testBean = (ITestBean) beanFactory.getBean("frozenBean");
    assertThat(((Advised) testBean).isFrozen()).isTrue();
  }

  @Test
  void customTargetSourceCreatorsApplyOnlyToConfiguredBeanNames() {
    ITestBean lazy1 = beanFactory.getBean("lazy1", ITestBean.class);
    ITestBean alias1 = beanFactory.getBean("lazy1alias", ITestBean.class);
    ITestBean lazy2 = beanFactory.getBean("lazy2", ITestBean.class);
    assertThat(AopUtils.isAopProxy(lazy1)).isTrue();
    assertThat(AopUtils.isAopProxy(alias1)).isTrue();
    assertThat(AopUtils.isAopProxy(lazy2)).isFalse();
  }

  private void jdkAssertions(ITestBean tb, int nopInterceptorCount) {
    NopInterceptor nop = (NopInterceptor) beanFactory.getBean("nopInterceptor");
    assertThat(nop.getCount()).isEqualTo(0);
    assertThat(AopUtils.isJdkDynamicProxy(tb)).isTrue();
    int age = 5;
    tb.setAge(age);
    assertThat(tb.getAge()).isEqualTo(age);
    assertThat(nop.getCount()).isEqualTo((2 * nopInterceptorCount));
  }

  /**
   * Also has counting before advice.
   */
  private void cglibAssertions(TestBean tb) {
    CountingBeforeAdvice cba = (CountingBeforeAdvice) beanFactory.getBean("countingBeforeAdvice");
    NopInterceptor nop = (NopInterceptor) beanFactory.getBean("nopInterceptor");
    assertThat(cba.getCalls()).isEqualTo(0);
    assertThat(nop.getCount()).isEqualTo(0);
    assertThat(AopUtils.isCglibProxy(tb)).isTrue();
    int age = 5;
    tb.setAge(age);
    assertThat(tb.getAge()).isEqualTo(age);
    assertThat(nop.getCount()).isEqualTo(2);
    assertThat(cba.getCalls()).isEqualTo(2);
  }

}

class CreatesTestBean implements FactoryBean<Object> {

  @Override
  public Object getObject() throws Exception {
    return new TestBean();
  }

  @Override
  public Class<?> getObjectType() {
    return TestBean.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
