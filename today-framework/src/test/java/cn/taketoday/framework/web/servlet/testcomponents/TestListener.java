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

package cn.taketoday.framework.web.servlet.testcomponents;

import java.io.IOException;
import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class TestListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    sce.getServletContext().addFilter("listenerAddedFilter", new ListenerAddedFilter())
            .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
    sce.getServletContext().setAttribute("listenerAttribute", "alpha");
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {

  }

  static class ListenerAddedFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
      request.setAttribute("listenerAddedFilterAttribute", "charlie");
      chain.doFilter(request, response);
    }

  }

}
