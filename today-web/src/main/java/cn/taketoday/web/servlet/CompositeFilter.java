/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * A generic composite servlet {@link Filter} that just delegates its behavior
 * to a chain (list) of user-supplied filters, achieving the functionality of a
 * {@link FilterChain}, but conveniently using only {@link Filter} instances.
 *
 * @author Dave Syer
 * @author TODAY 2021/9/11 18:47
 * @since 4.0
 */
public class CompositeFilter implements Filter {

  private ArrayList<? extends Filter> filters = new ArrayList<>();

  public void setFilters(List<? extends Filter> filters) {
    this.filters = new ArrayList<>(filters);
  }

  /**
   * Initialize all the filters, calling each one's init method in turn in the order supplied.
   *
   * @see Filter#init(FilterConfig)
   */
  @Override
  public void init(FilterConfig config) throws ServletException {
    for (Filter filter : this.filters) {
      filter.init(config);
    }
  }

  /**
   * Forms a temporary chain from the list of delegate filters supplied ({@link #setFilters})
   * and executes them in order. Each filter delegates to the next one in the list, achieving
   * the normal behavior of a {@link FilterChain}, despite the fact that this is a {@link Filter}.
   *
   * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
          throws IOException, ServletException {
    new VirtualFilterChain(chain, this.filters).doFilter(request, response);
  }

  /**
   * Clean up all the filters supplied, calling each one's destroy method in turn, but in reverse order.
   *
   * @see Filter#init(FilterConfig)
   */
  @Override
  public void destroy() {
    final ArrayList<? extends Filter> filters = this.filters;
    for (int i = filters.size(); i-- > 0; ) {
      Filter filter = filters.get(i);
      filter.destroy();
    }
  }

  private static class VirtualFilterChain implements FilterChain {

    private int currentPosition = 0;
    private final FilterChain originalChain;
    private final List<? extends Filter> additionalFilters;

    public VirtualFilterChain(FilterChain chain, List<? extends Filter> additionalFilters) {
      this.originalChain = chain;
      this.additionalFilters = additionalFilters;
    }

    @Override
    public void doFilter(
            final ServletRequest request, final ServletResponse response) throws IOException, ServletException {

      if (this.currentPosition == this.additionalFilters.size()) {
        this.originalChain.doFilter(request, response);
      }
      else {
        this.currentPosition++;
        Filter nextFilter = this.additionalFilters.get(this.currentPosition - 1);
        nextFilter.doFilter(request, response, this);
      }
    }
  }

}
