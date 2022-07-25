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

package cn.taketoday.web.servlet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import cn.taketoday.web.testfixture.servlet.MockFilterConfig;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockServletContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/9/11 18:50
 */
class CompositeFilterTests {

  @Test
  public void testCompositeFilter() throws ServletException, IOException {
    ServletContext sc = new MockServletContext();
    MockFilter targetFilter = new MockFilter();
    MockFilterConfig proxyConfig = new MockFilterConfig(sc);

    CompositeFilter filterProxy = new CompositeFilter();
    filterProxy.setFilters(Arrays.asList(targetFilter));
    filterProxy.init(proxyConfig);

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    filterProxy.doFilter(request, response, null);

    assertThat(targetFilter.filterConfig).isNotNull();
    assertThat(request.getAttribute("called")).isEqualTo(Boolean.TRUE);

    filterProxy.destroy();
    assertThat(targetFilter.filterConfig).isNull();
  }

  public static class MockFilter implements Filter {

    public FilterConfig filterConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
      this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) {
      request.setAttribute("called", Boolean.TRUE);
    }

    @Override
    public void destroy() {
      this.filterConfig = null;
    }
  }

}
