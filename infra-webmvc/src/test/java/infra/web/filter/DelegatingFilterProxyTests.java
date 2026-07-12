/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.filter;

import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.HttpContext;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DelegatingFilterProxy}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class DelegatingFilterProxyTests {

  private final MockHttpContext request = new MockHttpContext();

  private final FilterChain chain = mock(FilterChain.class);

  @Test
  void constructorWithDelegate() throws Throwable {
    Filter delegate = mock(Filter.class);
    DelegatingFilterProxy proxy = new DelegatingFilterProxy(delegate);

    proxy.doFilter(request, chain);

    verify(delegate).doFilter(request, chain);
  }

  @Test
  void constructorWithBeanNameAndContext() throws Throwable {
    Filter delegate = mock(Filter.class);
    ApplicationContext context = mock(ApplicationContext.class);
    given(context.getBean("myFilter", Filter.class)).willReturn(delegate);

    DelegatingFilterProxy proxy = new DelegatingFilterProxy("myFilter", context);
    proxy.afterPropertiesSet();

    proxy.doFilter(request, chain);

    verify(delegate).doFilter(request, chain);
  }

  @Test
  void lazyInitInDoFilter() throws Throwable {
    Filter delegate = mock(Filter.class);
    ApplicationContext context = mock(ApplicationContext.class);
    given(context.getBean("myFilter", Filter.class)).willReturn(delegate);

    DelegatingFilterProxy proxy = new DelegatingFilterProxy("myFilter", context);
    proxy.doFilter(request, chain);

    verify(delegate).doFilter(request, chain);
  }

  @Test
  void delegateInitializedOnlyOnce() throws Throwable {
    Filter delegate = mock(Filter.class);
    ApplicationContext context = mock(ApplicationContext.class);
    given(context.getBean("myFilter", Filter.class)).willReturn(delegate);

    DelegatingFilterProxy proxy = new DelegatingFilterProxy("myFilter", context);
    proxy.afterPropertiesSet();
    proxy.doFilter(request, chain);

    verify(context).getBean("myFilter", Filter.class);
    verify(delegate).doFilter(request, chain);
  }

  @Test
  void doFilterWithoutApplicationContextThrows() {
    DelegatingFilterProxy proxy = new DelegatingFilterProxy();
    proxy.setTargetBeanName("myFilter");

    assertThatThrownBy(() -> proxy.doFilter(request, chain))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No ApplicationContext set");
  }

  @Test
  void setAndGetTargetBeanName() {
    DelegatingFilterProxy proxy = new DelegatingFilterProxy();
    proxy.setTargetBeanName("customBean");
    assertThat(proxy.getTargetBeanName()).isEqualTo("customBean");
  }

  @Test
  void defaultConstructorHasNullTargetBean() {
    DelegatingFilterProxy proxy = new DelegatingFilterProxy();
    assertThat(proxy.getTargetBeanName()).isNull();
  }

  @Test
  void invokeDelegateCanBeOverridden() throws Throwable {
    Filter delegate = mock(Filter.class);
    DelegatingFilterProxy proxy = new DelegatingFilterProxy(delegate) {
      @Override
      protected void invokeDelegate(Filter delegate, HttpContext request, FilterChain filterChain) throws Exception {
        request.setAttribute("intercepted", Boolean.TRUE);
        super.invokeDelegate(delegate, request, filterChain);
      }
    };

    proxy.doFilter(request, chain);

    assertThat(request.getAttribute("intercepted")).isEqualTo(Boolean.TRUE);
    verify(delegate).doFilter(request, chain);
  }

  @Test
  void setApplicationContext() {
    ApplicationContext context = mock(ApplicationContext.class);
    DelegatingFilterProxy proxy = new DelegatingFilterProxy();
    proxy.setApplicationContext(context);
    assertThat(proxy).extracting("applicationContext").isSameAs(context);
  }

}
