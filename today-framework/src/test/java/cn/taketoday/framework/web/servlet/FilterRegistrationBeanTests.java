/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.io.IOException;
import java.util.EnumSet;

import cn.taketoday.framework.web.servlet.mock.MockFilter;
import cn.taketoday.web.servlet.filter.OncePerRequestFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link FilterRegistrationBean}.
 *
 * @author Phillip Webb
 */
class FilterRegistrationBeanTests extends AbstractFilterRegistrationBeanTests {

  private final MockFilter filter = new MockFilter();

  private final OncePerRequestFilter oncePerRequestFilter = new OncePerRequestFilter() {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
      filterChain.doFilter(request, response);
    }

  };

  @Test
  void setFilter() throws Exception {
    FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
    bean.setFilter(this.filter);
    bean.onStartup(this.servletContext);
    then(this.servletContext).should().addFilter("mockFilter", this.filter);
  }

  @Test
  void setFilterMustNotBeNull() {
    FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
    assertThatThrownBy(() -> bean.onStartup(this.servletContext))
            .isInstanceOf(IllegalStateException.class)
            .withFailMessage("Filter is required");
  }

  @Test
  void constructFilterMustNotBeNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> new FilterRegistrationBean<>(null))
            .withMessageContaining("Filter is required");
  }

  @Test
  void createServletRegistrationBeanMustNotBeNull() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new FilterRegistrationBean<>(this.filter, (ServletRegistrationBean[]) null))
            .withMessageContaining("ServletRegistrationBeans is required");
  }

  @Test
  void startupWithOncePerRequestDefaults() throws Exception {
    given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
    FilterRegistrationBean<?> bean = new FilterRegistrationBean<>(this.oncePerRequestFilter);
    bean.onStartup(this.servletContext);
    then(this.servletContext).should().addFilter(eq("oncePerRequestFilter"), eq(this.oncePerRequestFilter));
    then(this.registration).should().setAsyncSupported(true);
    then(this.registration).should().addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
  }

  @Override
  protected AbstractFilterRegistrationBean<MockFilter> createFilterRegistrationBean(
          ServletRegistrationBean<?>... servletRegistrationBeans) {
    return new FilterRegistrationBean<>(this.filter, servletRegistrationBeans);
  }

  @Override
  protected Filter getExpectedFilter() {
    return eq(this.filter);
  }

}
