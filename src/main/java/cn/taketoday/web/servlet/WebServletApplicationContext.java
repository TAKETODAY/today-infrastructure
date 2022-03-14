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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.servlet;

import cn.taketoday.web.WebApplicationContext;
import jakarta.servlet.ServletContext;

/**
 * Interface to provide configuration for a servlet web application. This is read-only while
 * the application is running, but may be reloaded if the implementation supports this.
 *
 * <p>This interface adds a {@code getServletContext()} method to the generic
 * ApplicationContext interface, and defines a well-known application attribute name
 * that the root context must be bound to in the bootstrap process.
 *
 * <p>Like generic application contexts, web application contexts are hierarchical.
 * There is a single root context per application, while each servlet in the application
 * (including a dispatcher servlet in the MVC framework) has its own child context.
 *
 * <p>In addition to standard application context lifecycle capabilities,
 * WebApplicationContext implementations need to detect {@link ServletContextAware}
 * beans and invoke the {@code setServletContext} method accordingly.
 *
 * @author TODAY
 * @since 2018-07-10 13:13:57
 */
public interface WebServletApplicationContext extends WebApplicationContext {

  /**
   * Context attribute to bind root WebApplicationContext to on successful startup.
   * <p>Note: If the startup of the root context fails, this attribute can contain
   * an exception or error as value. Use WebApplicationContextUtils for convenient
   * lookup of the root WebApplicationContext.
   *
   * @see cn.taketoday.web.context.support.WebApplicationContextUtils#getWebApplicationContext
   * @see cn.taketoday.web.context.support.WebApplicationContextUtils#getRequiredWebApplicationContext
   */
  String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";

  /**
   * Name of the ServletContext environment bean in the factory.
   *
   * @see jakarta.servlet.ServletContext
   */
  String SERVLET_CONTEXT_BEAN_NAME = "servletContext";

  /**
   * Name of the ServletContext init-params environment bean in the factory.
   * <p>Note: Possibly merged with ServletConfig parameters.
   * ServletConfig parameters override ServletContext parameters of the same name.
   *
   * @see jakarta.servlet.ServletContext#getInitParameterNames()
   * @see jakarta.servlet.ServletContext#getInitParameter(String)
   * @see jakarta.servlet.ServletConfig#getInitParameterNames()
   * @see jakarta.servlet.ServletConfig#getInitParameter(String)
   */
  String CONTEXT_PARAMETERS_BEAN_NAME = "contextParameters";

  /**
   * Name of the ServletContext attributes environment bean in the factory.
   *
   * @see jakarta.servlet.ServletContext#getAttributeNames()
   * @see jakarta.servlet.ServletContext#getAttribute(String)
   */
  String CONTEXT_ATTRIBUTES_BEAN_NAME = "contextAttributes";

  /**
   * Return the standard Servlet API ServletContext for this application.
   */
  ServletContext getServletContext();

}
