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

package cn.taketoday.framework.web.servlet;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.mock.web.MockFilterChain;
import cn.taketoday.mock.web.MockFilterConfig;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.web.context.support.GenericWebApplicationContext;
import cn.taketoday.web.context.support.GenericWebServletApplicationContext;
import cn.taketoday.web.servlet.filter.DelegatingFilterProxy;
import cn.taketoday.web.servlet.filter.GenericFilterBean;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.isA;

/**
 * Tests for {@link DelegatingFilterProxyRegistrationBean}.
 *
 * @author Phillip Webb
 */
class DelegatingFilterProxyRegistrationBeanTests extends AbstractFilterRegistrationBeanTests {

  private static ThreadLocal<Boolean> mockFilterInitialized = new ThreadLocal<>();

  private GenericWebApplicationContext applicationContext = new GenericWebServletApplicationContext(
          new MockServletContext());

  @Test
  void targetBeanNameMustNotBeNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> new DelegatingFilterProxyRegistrationBean(null))
            .withMessageContaining("TargetBeanName must not be null or empty");
  }

  @Test
  void targetBeanNameMustNotBeEmpty() {
    assertThatIllegalArgumentException().isThrownBy(() -> new DelegatingFilterProxyRegistrationBean(""))
            .withMessageContaining("TargetBeanName must not be null or empty");
  }

  @Test
  void nameDefaultsToTargetBeanName() {
    assertThat(new DelegatingFilterProxyRegistrationBean("myFilter").getOrDeduceName(null)).isEqualTo("myFilter");
  }

  @Test
  void getFilterUsesDelegatingFilterProxy() {
    DelegatingFilterProxyRegistrationBean registrationBean = createFilterRegistrationBean();
    Filter filter = registrationBean.getFilter();
    assertThat(filter).isInstanceOf(DelegatingFilterProxy.class);
    assertThat(filter).extracting("webApplicationContext").isEqualTo(this.applicationContext);
    assertThat(filter).extracting("targetBeanName").isEqualTo("mockFilter");
  }

  @Test
  void initShouldNotCauseEarlyInitialization() throws Exception {
    this.applicationContext.registerBeanDefinition("mockFilter", new RootBeanDefinition(MockFilter.class));
    DelegatingFilterProxyRegistrationBean registrationBean = createFilterRegistrationBean();
    Filter filter = registrationBean.getFilter();
    filter.init(new MockFilterConfig());
    assertThat(mockFilterInitialized.get()).isNull();
    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
    assertThat(mockFilterInitialized.get()).isTrue();
  }

  @Test
  void createServletRegistrationBeanMustNotBeNull() {
    assertThatIllegalArgumentException()
            .isThrownBy(
                    () -> new DelegatingFilterProxyRegistrationBean("mockFilter", (ServletRegistrationBean[]) null))
            .withMessageContaining("ServletRegistrationBeans must not be null");
  }

  @Override
  protected DelegatingFilterProxyRegistrationBean createFilterRegistrationBean(
          ServletRegistrationBean<?>... servletRegistrationBeans) {
    DelegatingFilterProxyRegistrationBean bean = new DelegatingFilterProxyRegistrationBean("mockFilter",
            servletRegistrationBeans);
    bean.setApplicationContext(this.applicationContext);
    return bean;
  }

  @Override
  protected Filter getExpectedFilter() {
    return isA(DelegatingFilterProxy.class);
  }

  static class MockFilter extends GenericFilterBean {

    MockFilter() {
      mockFilterInitialized.set(true);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    }

  }

}
