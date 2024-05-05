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

package cn.taketoday.mock.api;

import java.io.IOException;

/**
 * A FilterChain is an object provided by the servlet container to the developer giving a view into the invocation chain
 * of a filtered request for a resource. Filters use the FilterChain to invoke the next filter in the chain, or if the
 * calling filter is the last filter in the chain, to invoke the resource at the end of the chain.
 *
 * @see Filter
 * @since Servlet 2.3
 */
public interface FilterChain {

  /**
   * Causes the next filter in the chain to be invoked, or if the calling filter is the last filter in the chain, causes
   * the resource at the end of the chain to be invoked.
   *
   * @param request the request to pass along the chain.
   * @param response the response to pass along the chain.
   * @throws IOException if an I/O related error has occurred during the processing
   * @throws ServletException if an exception has occurred that interferes with the filterChain's normal operation
   */
  void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException;

}
