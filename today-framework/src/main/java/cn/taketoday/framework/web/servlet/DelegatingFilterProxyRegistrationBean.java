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

import cn.taketoday.beans.BeansException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.servlet.filter.DelegatingFilterProxy;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * A {@link ServletContextInitializer} to register {@link DelegatingFilterProxy}s in a
 * Servlet 3.0+ container. Similar to the {@link ServletContext#addFilter(String, Filter)
 * registration} features provided by {@link ServletContext} but with a Framework Bean
 * friendly design.
 * <p>
 * The bean name of the actual delegate {@link Filter} should be specified using the
 * {@code targetBeanName} constructor argument. Unlike the {@link FilterRegistrationBean},
 * referenced filters are not instantiated early. In fact, if the delegate filter bean is
 * marked {@code @Lazy} it won't be instantiated at all until the filter is called.
 * <p>
 * Registrations can be associated with {@link #setUrlPatterns URL patterns} and/or
 * servlets (either by {@link #setServletNames name} or via a
 * {@link #setServletRegistrationBeans ServletRegistrationBean}s). When no URL pattern or
 * servlets are specified the filter will be associated to '/*'. The targetBeanName will
 * be used as the filter name if not otherwise specified.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ServletContextInitializer
 * @see ServletContext#addFilter(String, Filter)
 * @see FilterRegistrationBean
 * @see DelegatingFilterProxy
 * @since 4.0
 */
public class DelegatingFilterProxyRegistrationBean extends AbstractFilterRegistrationBean<DelegatingFilterProxy>
        implements ApplicationContextAware {

  private ApplicationContext applicationContext;

  private final String targetBeanName;

  /**
   * Create a new {@link DelegatingFilterProxyRegistrationBean} instance to be
   * registered with the specified {@link ServletRegistrationBean}s.
   *
   * @param targetBeanName name of the target filter bean to look up in the
   * application context (must not be {@code null}).
   * @param servletRegistrationBeans associate {@link ServletRegistrationBean}s
   */
  public DelegatingFilterProxyRegistrationBean(
          String targetBeanName, ServletRegistrationBean<?>... servletRegistrationBeans) {
    super(servletRegistrationBeans);
    Assert.hasLength(targetBeanName, "TargetBeanName must not be null or empty");
    this.targetBeanName = targetBeanName;
    setName(targetBeanName);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  protected String getTargetBeanName() {
    return this.targetBeanName;
  }

  @Override
  public DelegatingFilterProxy getFilter() {
    return new DelegatingFilterProxy(this.targetBeanName, getApplicationContext()) {

      @Override
      protected void initFilterBean() throws ServletException {
        // Don't initialize filter bean on init()
      }

    };
  }

  private ApplicationContext getApplicationContext() {
    Assert.state(this.applicationContext != null, "No ApplicationContext");
    return this.applicationContext;
  }

}
