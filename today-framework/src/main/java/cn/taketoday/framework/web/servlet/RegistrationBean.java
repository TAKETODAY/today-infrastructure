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

import cn.taketoday.core.Ordered;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * Base class for Servlet 3.0+ based registration beans.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ServletRegistrationBean
 * @see FilterRegistrationBean
 * @see DelegatingFilterProxyRegistrationBean
 * @see ServletListenerRegistrationBean
 * @since 4.0
 */
public abstract class RegistrationBean implements ServletContextInitializer, Ordered {

  private int order = Ordered.LOWEST_PRECEDENCE;

  private boolean enabled = true;

  @Override
  public final void onStartup(ServletContext servletContext) throws ServletException {
    String description = getDescription();
    if (isEnabled()) {
      register(description, servletContext);
    }
    else {
      LoggerFactory.getLogger(getClass())
              .info("{} was not registered (disabled)", StringUtils.capitalize(description));
    }
  }

  /**
   * Return a description of the registration. For example "Servlet resourceServlet"
   *
   * @return a description of the registration
   */
  protected abstract String getDescription();

  /**
   * Register this bean with the servlet context.
   *
   * @param description a description of the item being registered
   * @param servletContext the servlet context
   */
  protected abstract void register(String description, ServletContext servletContext);

  /**
   * Flag to indicate that the registration is enabled.
   *
   * @param enabled the enabled to set
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Return if the registration is enabled.
   *
   * @return if enabled (default {@code true})
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * Set the order of the registration bean.
   *
   * @param order the order
   */
  public void setOrder(int order) {
    this.order = order;
  }

  /**
   * Get the order of the registration bean.
   *
   * @return the order
   */
  @Override
  public int getOrder() {
    return this.order;
  }

}
