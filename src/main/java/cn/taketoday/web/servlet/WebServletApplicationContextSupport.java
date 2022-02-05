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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.web.ServletContextAware;
import cn.taketoday.web.WebApplicationContextSupport;
import jakarta.servlet.ServletContext;

/**
 * @author TODAY <br>
 * 2019-12-21 15:56
 */
public class WebServletApplicationContextSupport
        extends WebApplicationContextSupport implements ServletContextAware {

  private ServletContext servletContext;

  @Override
  public final void setServletContext(ServletContext servletContext) {
    if (servletContext != this.servletContext) {
      initServletContext(this.servletContext = servletContext);
    }
  }

  /**
   * Subclasses may override this for custom initialization based on the
   * ServletContext that this application object runs in.
   * <p>
   * The default implementation is empty. Called by
   * {@link #initApplicationContext(ApplicationContext)} as well as
   * {@link #setServletContext(jakarta.servlet.ServletContext)}.
   *
   * @param servletContext the ServletContext that this application object runs in (never
   * {@code null})
   */
  protected void initServletContext(ServletContext servletContext) { }

  /**
   * Return the current application context as
   * {@link WebServletApplicationContext}.
   *
   * @throws IllegalStateException if not running in a WebApplicationContext
   * @see #getApplicationContext()
   */
  public final WebServletApplicationContext getWebServletApplicationContext() {
    ApplicationContext ctx = getApplicationContext();
    if (ctx instanceof WebServletApplicationContext) {
      return (WebServletApplicationContext) ctx;
    }
    throw new IllegalStateException("ApplicationContext must be a WebServletApplicationContext");
  }

  /**
   * Return the current ServletContext.
   *
   * @throws IllegalStateException if not running within a required ServletContext
   */
  public final ServletContext getServletContext() {
    if (this.servletContext != null) {
      return this.servletContext;
    }
    final WebServletApplicationContext context = getWebServletApplicationContext();
    ServletContext servletContext = context.getServletContext();
    if (servletContext == null) {
      throw new IllegalStateException(
              "WebServletApplicationContextSupport instance [" + this +
                      "] does not run within a ServletContext. Make sure the object is fully configured!");
    }
    return servletContext;
  }
}
