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

package infra.aop.interceptor;

import org.junit.jupiter.api.Test;

import infra.aop.framework.ProxyFactory;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.beans.factory.NamedBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ExposeBeanNameAdvisorsTests {

  private class RequiresBeanNameBoundTestBean extends TestBean {
    private final String beanName;

    public RequiresBeanNameBoundTestBean(String beanName) {
      this.beanName = beanName;
    }

    @Override
    public int getAge() {
      assertThat(ExposeBeanNameAdvisors.getBeanName()).isEqualTo(beanName);
      return super.getAge();
    }
  }

  @Test
  public void testNoIntroduction() {
    String beanName = "foo";
    TestBean target = new RequiresBeanNameBoundTestBean(beanName);
    ProxyFactory pf = new ProxyFactory(target);
    pf.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
    pf.addAdvisor(ExposeBeanNameAdvisors.createAdvisorWithoutIntroduction(beanName));
    ITestBean proxy = (ITestBean) pf.getProxy();

    boolean condition = proxy instanceof NamedBean;
    assertThat(condition).as("No introduction").isFalse();
    // Requires binding
    proxy.getAge();
  }

  @Test
  public void testWithIntroduction() {
    String beanName = "foo";
    TestBean target = new RequiresBeanNameBoundTestBean(beanName);
    ProxyFactory pf = new ProxyFactory(target);
    pf.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
    pf.addAdvisor(ExposeBeanNameAdvisors.createAdvisorIntroducingNamedBean(beanName));
    ITestBean proxy = (ITestBean) pf.getProxy();

    boolean condition = proxy instanceof NamedBean;
    assertThat(condition).as("Introduction was made").isTrue();
    // Requires binding
    proxy.getAge();

    NamedBean nb = (NamedBean) proxy;
    assertThat(nb.getBeanName()).as("Name returned correctly").isEqualTo(beanName);
  }

}
