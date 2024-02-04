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

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.aop.interceptor.DebugInterceptor;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests lookup methods wrapped by a CGLIB proxy (see SPR-391).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class LookupMethodWrappedByCglibProxyTests {

  private static final Class<?> CLASS = LookupMethodWrappedByCglibProxyTests.class;
  private static final String CLASSNAME = CLASS.getSimpleName();

  private static final String CONTEXT = CLASSNAME + "-context.xml";

  private ApplicationContext applicationContext;

  @BeforeEach
  void setUp() {
    this.applicationContext = new ClassPathXmlApplicationContext(CONTEXT, CLASS);
    resetInterceptor();
  }

  @Test
  void testAutoProxiedLookup() {
    OverloadLookup olup = (OverloadLookup) applicationContext.getBean("autoProxiedOverload");
    ITestBean jenny = olup.newTestBean();
    assertThat(jenny.getName()).isEqualTo("Jenny");
    assertThat(olup.testMethod()).isEqualTo("foo");
    assertInterceptorCount(2);
  }

  @Test
  void testRegularlyProxiedLookup() {
    OverloadLookup olup = (OverloadLookup) applicationContext.getBean("regularlyProxiedOverload");
    ITestBean jenny = olup.newTestBean();
    assertThat(jenny.getName()).isEqualTo("Jenny");
    assertThat(olup.testMethod()).isEqualTo("foo");
    assertInterceptorCount(2);
  }

  private void assertInterceptorCount(int count) {
    DebugInterceptor interceptor = getInterceptor();
    assertThat(interceptor.getCount()).as("Interceptor count is incorrect").isEqualTo(count);
  }

  private void resetInterceptor() {
    DebugInterceptor interceptor = getInterceptor();
    interceptor.resetCount();
  }

  private DebugInterceptor getInterceptor() {
    return (DebugInterceptor) applicationContext.getBean("interceptor");
  }

}

abstract class OverloadLookup {

  public abstract ITestBean newTestBean();

  public String testMethod() {
    return "foo";
  }
}

