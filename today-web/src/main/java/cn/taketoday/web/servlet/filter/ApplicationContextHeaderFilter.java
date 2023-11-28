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

package cn.taketoday.web.servlet.filter;

import java.io.IOException;

import cn.taketoday.context.ApplicationContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link OncePerRequestFilter} to add an {@literal X-Application-Context} header that
 * contains the {@link ApplicationContext#getId() ApplicationContext ID}.
 *
 * @author Phillip Webb
 * @author Venil Noronha
 * @since 4.0
 */
public class ApplicationContextHeaderFilter extends OncePerRequestFilter {

  /**
   * Public constant for {@literal X-Application-Context}.
   */
  public static final String HEADER_NAME = "X-Application-Context";

  private final ApplicationContext applicationContext;

  public ApplicationContextHeaderFilter(ApplicationContext context) {
    this.applicationContext = context;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
    response.addHeader(HEADER_NAME, this.applicationContext.getId());
    filterChain.doFilter(request, response);
  }

}
