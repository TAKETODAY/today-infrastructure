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

package cn.taketoday.web.context.support;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.servlet.ServletConfigAware;
import cn.taketoday.web.servlet.ServletContextAware;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

/**
 * {@link BeanPostProcessor} implementation
 * that passes the ServletContext to beans that implement the
 * {@link ServletContextAware} interface.
 *
 * <p>Web application contexts will automatically register this with their
 * underlying bean factory. Applications do not use this directly.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.web.servlet.ServletContextAware
 * @since 4.0 2022/2/20 20:57
 */
public class ServletContextAwareProcessor implements InitializationBeanPostProcessor {

  @Nullable
  private ServletContext servletContext;

  @Nullable
  private ServletConfig servletConfig;

  /**
   * Create a new ServletContextAwareProcessor without an initial context or config.
   * When this constructor is used the {@link #getServletContext()} and/or
   * {@link #getServletConfig()} methods should be overridden.
   */
  protected ServletContextAwareProcessor() { }

  /**
   * Create a new ServletContextAwareProcessor for the given context.
   */
  public ServletContextAwareProcessor(ServletContext servletContext) {
    this(servletContext, null);
  }

  /**
   * Create a new ServletContextAwareProcessor for the given config.
   */
  public ServletContextAwareProcessor(ServletConfig servletConfig) {
    this(null, servletConfig);
  }

  /**
   * Create a new ServletContextAwareProcessor for the given context and config.
   */
  public ServletContextAwareProcessor(@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {
    this.servletContext = servletContext;
    this.servletConfig = servletConfig;
  }

  /**
   * Returns the {@link ServletContext} to be injected or {@code null}. This method
   * can be overridden by subclasses when a context is obtained after the post-processor
   * has been registered.
   */
  @Nullable
  protected ServletContext getServletContext() {
    if (this.servletContext == null && getServletConfig() != null) {
      return getServletConfig().getServletContext();
    }
    return this.servletContext;
  }

  /**
   * Returns the {@link ServletConfig} to be injected or {@code null}. This method
   * can be overridden by subclasses when a context is obtained after the post-processor
   * has been registered.
   */
  @Nullable
  protected ServletConfig getServletConfig() {
    return this.servletConfig;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (getServletContext() != null && bean instanceof ServletContextAware) {
      ((ServletContextAware) bean).setServletContext(getServletContext());
    }
    if (getServletConfig() != null && bean instanceof ServletConfigAware) {
      ((ServletConfigAware) bean).setServletConfig(getServletConfig());
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    return bean;
  }

}

