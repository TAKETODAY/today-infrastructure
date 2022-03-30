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

package cn.taketoday.framework.web.servlet.support;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.web.context.ConfigurableWebServletApplicationContext;
import cn.taketoday.web.servlet.WebServletApplicationContext;
import jakarta.servlet.ServletContext;

/**
 * {@link ApplicationContextInitializer} for setting the servlet context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/30 15:23
 */
public class ServletContextApplicationContextInitializer implements ApplicationContextInitializer, Ordered {

  private int order = Ordered.HIGHEST_PRECEDENCE;

  private final ServletContext servletContext;

  private final boolean addApplicationContextAttribute;

  /**
   * Create a new {@link ServletContextApplicationContextInitializer} instance.
   *
   * @param servletContext the servlet that should be ultimately set.
   */
  public ServletContextApplicationContextInitializer(ServletContext servletContext) {
    this(servletContext, false);
  }

  /**
   * Create a new {@link ServletContextApplicationContextInitializer} instance.
   *
   * @param servletContext the servlet that should be ultimately set.
   * @param addApplicationContextAttribute if the {@link ApplicationContext} should be
   * stored as an attribute in the {@link ServletContext}
   */
  public ServletContextApplicationContextInitializer(ServletContext servletContext,
          boolean addApplicationContextAttribute) {
    this.servletContext = servletContext;
    this.addApplicationContextAttribute = addApplicationContextAttribute;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    if (applicationContext instanceof ConfigurableWebServletApplicationContext context) {
      context.setServletContext(this.servletContext);
      if (this.addApplicationContextAttribute) {
        this.servletContext.setAttribute(WebServletApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
      }
    }
  }

}
