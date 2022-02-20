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

import cn.taketoday.lang.Assert;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;

/**
 * A {@link ServletContextInitializer} to register {@link Filter}s in a Servlet 3.0+
 * container. Similar to the {@link ServletContext#addFilter(String, Filter) registration}
 * features provided by {@link ServletContext} but with a Spring Bean friendly design.
 * <p>
 * The {@link #setFilter(Filter) Filter} must be specified before calling
 * {@link #onStartup(ServletContext)}. Registrations can be associated with
 * {@link #setUrlPatterns URL patterns} and/or servlets (either by {@link #setServletNames
 * name} or via a {@link #setServletRegistrationBeans ServletRegistrationBean}s). When no
 * URL pattern or servlets are specified the filter will be associated to '/*'. The filter
 * name will be deduced if not specified.
 *
 * @param <T> the type of {@link Filter} to register
 * @author Phillip Webb
 * @see ServletContextInitializer
 * @see ServletContext#addFilter(String, Filter)
 * @see DelegatingFilterProxyRegistrationBean
 * @since 1.4.0
 */
public class FilterRegistrationBean<T extends Filter> extends AbstractFilterRegistrationBean<T> {

  private T filter;

  /**
   * Create a new {@link FilterRegistrationBean} instance.
   */
  public FilterRegistrationBean() {
  }

  /**
   * Create a new {@link FilterRegistrationBean} instance to be registered with the
   * specified {@link ServletRegistrationBean}s.
   *
   * @param filter the filter to register
   * @param servletRegistrationBeans associate {@link ServletRegistrationBean}s
   */
  public FilterRegistrationBean(T filter, ServletRegistrationBean<?>... servletRegistrationBeans) {
    super(servletRegistrationBeans);
    Assert.notNull(filter, "Filter must not be null");
    this.filter = filter;
  }

  @Override
  public T getFilter() {
    return this.filter;
  }

  /**
   * Set the filter to be registered.
   *
   * @param filter the filter
   */
  public void setFilter(T filter) {
    Assert.notNull(filter, "Filter must not be null");
    this.filter = filter;
  }

}
