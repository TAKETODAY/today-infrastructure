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

package cn.taketoday.annotation.config.web.servlet;

import java.util.Collection;

import cn.taketoday.framework.web.servlet.ServletRegistrationBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.servlet.DispatcherServlet;

/**
 * {@link ServletRegistrationBean} for the auto-configured {@link DispatcherServlet}. Both
 * registers the servlet and exposes {@link DispatcherServletPath} information.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/2 21:50
 */
public class DispatcherServletRegistrationBean
        extends ServletRegistrationBean<DispatcherServlet> implements DispatcherServletPath {

  private final String path;

  /**
   * Create a new {@link DispatcherServletRegistrationBean} instance for the given
   * servlet and path.
   *
   * @param servlet the dispatcher servlet
   * @param path the dispatcher servlet path
   */
  public DispatcherServletRegistrationBean(DispatcherServlet servlet, String path) {
    super(servlet);
    Assert.notNull(path, "Path is required");
    this.path = path;
    super.addUrlMappings(getServletUrlMapping());
  }

  @Override
  public String getPath() {
    return this.path;
  }

  @Override
  public void setUrlMappings(Collection<String> urlMappings) {
    throw new UnsupportedOperationException("URL Mapping cannot be changed on a DispatcherServlet registration");
  }

  @Override
  public void addUrlMappings(String... urlMappings) {
    throw new UnsupportedOperationException("URL Mapping cannot be changed on a DispatcherServlet registration");
  }

}

