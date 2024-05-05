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

package cn.taketoday.mock.web;

import java.io.IOException;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.Filter;
import cn.taketoday.mock.api.FilterChain;
import cn.taketoday.mock.api.Servlet;
import cn.taketoday.mock.api.ServletException;
import cn.taketoday.mock.api.ServletRequest;
import cn.taketoday.mock.api.ServletResponse;

/**
 * Implementation of the {@link cn.taketoday.mock.api.FilterConfig} interface which
 * simply passes the call through to a given Filter/FilterChain combination
 * (indicating the next Filter in the chain along with the FilterChain that it is
 * supposed to work on) or to a given Servlet (indicating the end of the chain).
 *
 * @author Juergen Hoeller
 * @see Filter
 * @see Servlet
 * @see MockFilterChain
 * @since 4.0
 */
public class PassThroughFilterChain implements FilterChain {

  @Nullable
  private Filter filter;

  @Nullable
  private FilterChain nextFilterChain;

  @Nullable
  private Servlet servlet;

  /**
   * Create a new PassThroughFilterChain that delegates to the given Filter,
   * calling it with the given FilterChain.
   *
   * @param filter the Filter to delegate to
   * @param nextFilterChain the FilterChain to use for that next Filter
   */
  public PassThroughFilterChain(Filter filter, FilterChain nextFilterChain) {
    Assert.notNull(filter, "Filter is required");
    Assert.notNull(nextFilterChain, "'FilterChain is required");
    this.filter = filter;
    this.nextFilterChain = nextFilterChain;
  }

  /**
   * Create a new PassThroughFilterChain that delegates to the given Servlet.
   *
   * @param servlet the Servlet to delegate to
   */
  public PassThroughFilterChain(Servlet servlet) {
    Assert.notNull(servlet, "Servlet is required");
    this.servlet = servlet;
  }

  /**
   * Pass the call on to the Filter/Servlet.
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response) throws ServletException, IOException {
    if (this.filter != null) {
      this.filter.doFilter(request, response, this.nextFilterChain);
    }
    else {
      Assert.state(this.servlet != null, "Neither a Filter not a Servlet set");
      this.servlet.service(request, response);
    }
  }

}
