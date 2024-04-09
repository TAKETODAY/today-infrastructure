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

package cn.taketoday.web.servlet;

import java.io.File;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ApplicationObjectSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import jakarta.servlet.ServletContext;

/**
 * Convenient superclass for application objects running in a {@link WebApplicationContext}.
 * Provides {@code getWebApplicationContext()}, {@code getServletContext()}, and
 * {@code getTempDir()} accessors.
 *
 * <p>Note: It is generally recommended to use individual callback interfaces for the actual
 * callbacks needed. This broad base class is primarily intended for use within the framework,
 * in case of {@link ServletContext} access etc typically being needed.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-12-27 09:36
 */
public class WebApplicationObjectSupport extends ApplicationObjectSupport implements ServletContextAware {

  @Nullable
  private ServletContext servletContext;

  @Override
  public final void setServletContext(ServletContext servletContext) {
    if (servletContext != this.servletContext) {
      this.servletContext = servletContext;
      initServletContext(servletContext);
    }
  }

  /**
   * Overrides the base class behavior to enforce running in an ApplicationContext.
   * All accessors will throw IllegalStateException if not running in a context.
   *
   * @see #getApplicationContext()
   * @see #getMessageSourceAccessor()
   * @see #getWebApplicationContext()
   */
  @Override
  protected boolean isContextRequired() {
    return true;
  }

  /**
   * Calls {@link #initServletContext(jakarta.servlet.ServletContext)} if the
   * given ApplicationContext is a {@link WebApplicationContext}.
   */
  @Override
  protected void initApplicationContext(ApplicationContext context) {
    super.initApplicationContext(context);
    if (this.servletContext == null && context instanceof WebApplicationContext wac) {
      this.servletContext = wac.getServletContext();
      if (this.servletContext != null) {
        initServletContext(this.servletContext);
      }
    }
  }

  /**
   * Subclasses may override this for custom initialization based
   * on the ServletContext that this application object runs in.
   * <p>The default implementation is empty. Called by
   * {@link #initApplicationContext(ApplicationContext)}
   * as well as {@link #setServletContext(jakarta.servlet.ServletContext)}.
   *
   * @param servletContext the ServletContext that this application object runs in
   * (never {@code null})
   */
  protected void initServletContext(ServletContext servletContext) {
  }

  /**
   * Return the current application context as WebApplicationContext.
   * <p><b>NOTE:</b> Only use this if you actually need to access
   * WebApplicationContext-specific functionality. Preferably use
   * {@code getApplicationContext()} or {@code getServletContext()}
   * else, to be able to run in non-WebApplicationContext environments as well.
   *
   * @throws IllegalStateException if not running in a WebApplicationContext
   * @see #getApplicationContext()
   */
  @Nullable
  protected final WebApplicationContext getWebApplicationContext() throws IllegalStateException {
    ApplicationContext ctx = getApplicationContext();
    if (ctx instanceof WebApplicationContext wac) {
      return wac;
    }
    else if (isContextRequired()) {
      throw new IllegalStateException("WebApplicationObjectSupport instance [" + this +
              "] does not run in a WebApplicationContext but in: " + ctx);
    }
    else {
      return null;
    }
  }

  /**
   * Return the current ServletContext.
   *
   * @throws IllegalStateException if not running within a required ServletContext
   * @see #isContextRequired()
   */
  @Nullable
  protected final ServletContext getServletContext() throws IllegalStateException {
    if (this.servletContext != null) {
      return this.servletContext;
    }
    ServletContext servletContext = null;
    WebApplicationContext wac = getWebApplicationContext();
    if (wac != null) {
      servletContext = wac.getServletContext();
    }
    if (servletContext == null && isContextRequired()) {
      throw new IllegalStateException(
              "WebApplicationObjectSupport instance [%s] does not run within a ServletContext. Make sure the object is fully configured!"
                      .formatted(this));
    }
    return servletContext;
  }

  /**
   * Return the temporary directory for the current web application,
   * as provided by the servlet container.
   *
   * @return the File representing the temporary directory
   * @throws IllegalStateException if not running within a ServletContext
   * @see ServletUtils#getTempDir(jakarta.servlet.ServletContext)
   */
  protected final File getTempDir() throws IllegalStateException {
    ServletContext servletContext = getServletContext();
    Assert.state(servletContext != null, "ServletContext is required");
    return ServletUtils.getTempDir(servletContext);
  }

}
