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

package cn.taketoday.web.mock;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockConfig;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.web.mock.support.WebApplicationContextUtils;

/**
 * Interface to provide configuration for a servlet web application. This is read-only while
 * the application is running, but may be reloaded if the implementation supports this.
 *
 * <p>This interface adds a {@code getMockContext()} method to the generic
 * ApplicationContext interface, and defines a well-known application attribute name
 * that the root context must be bound to in the bootstrap process.
 *
 * <p>Like generic application contexts, web application contexts are hierarchical.
 * There is a single root context per application, while each servlet in the application
 * (including a dispatcher servlet in the MVC framework) has its own child context.
 *
 * <p>In addition to standard application context lifecycle capabilities,
 * WebApplicationContext implementations need to detect {@link MockContextAware}
 * beans and invoke the {@code setMockContext} method accordingly.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MockContextAware#setMockContext
 * @since 2019-07-10 22:03
 */
public interface WebApplicationContext extends ApplicationContext {

  /**
   * Context attribute to bind root WebApplicationContext to on successful startup.
   * <p>Note: If the startup of the root context fails, this attribute can contain
   * an exception or error as value. Use WebApplicationContextUtils for convenient
   * lookup of the root WebApplicationContext.
   *
   * @see WebApplicationContextUtils#getWebApplicationContext
   * @see WebApplicationContextUtils#getRequiredWebApplicationContext
   */
  String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";

  /**
   * Name of the MockContext environment bean in the factory.
   *
   * @see MockContext
   */
  String SERVLET_CONTEXT_BEAN_NAME = "mockContext";

  /**
   * Name of the MockContext init-params environment bean in the factory.
   * <p>Note: Possibly merged with ServletConfig parameters.
   * ServletConfig parameters override MockContext parameters of the same name.
   *
   * @see MockContext#getInitParameterNames()
   * @see MockContext#getInitParameter(String)
   * @see MockConfig#getInitParameterNames()
   * @see MockConfig#getInitParameter(String)
   */
  String CONTEXT_PARAMETERS_BEAN_NAME = "contextParameters";

  /**
   * Name of the MockContext attributes environment bean in the factory.
   *
   * @see MockContext#getAttributeNames()
   * @see MockContext#getAttribute(String)
   */
  String CONTEXT_ATTRIBUTES_BEAN_NAME = "contextAttributes";

  /**
   * Return the standard Servlet API MockContext for this application.
   */
  @Nullable
  MockContext getMockContext();

}
