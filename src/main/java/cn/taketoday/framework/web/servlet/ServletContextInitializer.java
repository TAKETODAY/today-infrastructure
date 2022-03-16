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

import cn.taketoday.web.config.WebApplicationInitializer;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * Interface used to configure a Servlet 3.0+ {@link ServletContext context}
 * programmatically.
 * <p>
 * This interface is designed to act in a similar way to
 * {@link ServletContainerInitializer}, but have a lifecycle that's managed by Framework and
 * not the Servlet container.
 * <p>
 * For configuration examples see {@link WebApplicationInitializer}.
 *
 * @author Phillip Webb
 * @see WebApplicationInitializer
 * @since 4.0
 */
@FunctionalInterface
public interface ServletContextInitializer {

  /**
   * Configure the given {@link ServletContext} with any servlets, filters, listeners
   * context-params and attributes necessary for initialization.
   *
   * @param servletContext the {@code ServletContext} to initialize
   * @throws ServletException if any call against the given {@code ServletContext}
   * throws a {@code ServletException}
   */
  void onStartup(ServletContext servletContext) throws ServletException;

}
