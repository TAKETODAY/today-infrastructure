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

package cn.taketoday.web.servlet.support;

import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.web.servlet.WebApplicationContext;
import jakarta.servlet.ServletContext;

/**
 * ResourceLoader implementation that resolves paths as ServletContext
 * resources, for use outside a WebApplicationContext (for example,
 * in an HttpServletBean or GenericFilterBean subclass).
 *
 * <p>Within a WebApplicationContext, resource paths are automatically
 * resolved as ServletContext resources by the context implementation.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getResourceByPath
 * @see ServletContextResource
 * @see WebApplicationContext
 * @see cn.taketoday.web.servlet.filter.GenericFilterBean
 * @since 4.0 2022/2/20 16:16
 */
public class ServletContextResourceLoader extends DefaultResourceLoader {

  private final ServletContext servletContext;

  /**
   * Create a new ServletContextResourceLoader.
   *
   * @param servletContext the ServletContext to load resources with
   */
  public ServletContextResourceLoader(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  /**
   * This implementation supports file paths beneath the root of the web application.
   *
   * @see ServletContextResource
   */
  @Override
  protected Resource getResourceByPath(String path) {
    return new ServletContextResource(servletContext, path);
  }

}

