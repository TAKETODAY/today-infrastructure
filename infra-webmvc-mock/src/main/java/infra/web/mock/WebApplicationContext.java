/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.mock;

import org.jspecify.annotations.Nullable;

import infra.context.ApplicationContext;
import infra.mock.api.MockContext;

/**
 * Interface to provide configuration for a web mvc application. This is read-only while
 * the application is running, but may be reloaded if the implementation supports this.
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
 * @since 2019-07-10 22:03
 */
public interface WebApplicationContext extends ApplicationContext {

  /**
   * Context attribute to bind root WebApplicationContext to on successful startup.
   * <p>Note: If the startup of the root context fails, this attribute can contain
   * an exception or error as value. Use WebApplicationContextUtils for convenient
   * lookup of the root WebApplicationContext.
   */
  String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";

  /**
   * Name of the MockContext environment bean in the factory.
   *
   * @see MockContext
   */
  String MOCK_CONTEXT_BEAN_NAME = "mockContext";

  String CONTEXT_PARAMETERS_BEAN_NAME = "contextParameters";

  /**
   * Name of the MockContext attributes environment bean in the factory.
   */
  String CONTEXT_ATTRIBUTES_BEAN_NAME = "contextAttributes";

  /**
   * Return the standard Servlet API MockContext for this application.
   */
  @Nullable
  MockContext getMockContext();

}
