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

package cn.taketoday.web;

import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.servlet.ServletContextAwareBeanPostProcessor;
import cn.taketoday.web.servlet.WebServletApplicationContext;
import jakarta.servlet.ServletContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/7 13:52
 */
public class StaticWebApplicationContext extends StaticApplicationContext implements WebServletApplicationContext {

  @Nullable
  private ServletContext servletContext;

  @Nullable
  private String namespace;

  public StaticWebApplicationContext() {
  }

  /**
   * Set the ServletContext that this WebApplicationContext runs in.
   */
  @Override
  public void setServletContext(@Nullable ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  @Override
  @Nullable
  public ServletContext getServletContext() {
    return this.servletContext;
  }

  @Override
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    beanFactory.addBeanPostProcessor(new ServletContextAwareBeanPostProcessor(this));
    beanFactory.ignoreDependencyInterface(ServletContextAware.class);
  }

  @Override
  public String getContextPath() {
    return servletContext.getContextPath();
  }

}
