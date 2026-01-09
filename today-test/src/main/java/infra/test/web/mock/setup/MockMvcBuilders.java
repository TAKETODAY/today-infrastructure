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

package infra.test.web.mock.setup;

import infra.context.ApplicationContext;
import infra.mock.api.MockContext;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.MockMvcBuilder;
import infra.web.mock.MockDispatcher;
import infra.web.mock.WebApplicationContext;

/**
 * The main class to import in order to access all available {@link MockMvcBuilder MockMvcBuilders}.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to
 * this setting, open the Preferences and type "favorites".
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see #webAppContextSetup(ApplicationContext)
 * @see #standaloneSetup(Object...)
 * @since 4.0
 */
public final class MockMvcBuilders {

  private MockMvcBuilders() { }

  /**
   * Build a {@link MockMvc} instance using the given, fully initialized
   * (i.e., <em>refreshed</em>) {@link WebApplicationContext}.
   * <p>The {@link MockDispatcher DispatcherHandler}
   * will use the context to discover Web MVC infrastructure and application
   * controllers in it. The context must have been configured with a
   * {@link MockContext MockContext}.
   */
  public static DefaultMockMvcBuilder webAppContextSetup(ApplicationContext context) {
    return new DefaultMockMvcBuilder(context);
  }

  /**
   * Build a {@link MockMvc} instance by registering one or more
   * {@code @Controller} instances and configuring Web MVC infrastructure
   * programmatically.
   * <p>This allows full control over the instantiation and initialization of
   * controllers and their dependencies, similar to plain unit tests while
   * also making it possible to test one controller at a time.
   * <p>When this builder is used, the minimum infrastructure required by the
   * {@link MockDispatcher DispatcherServlet}
   * to serve requests with annotated controllers is created automatically
   * and can be customized, resulting in configuration that is equivalent to
   * what MVC Java configuration provides except using builder-style methods.
   * <p>If the Web MVC configuration of an application is relatively
   * straight-forward &mdash; for example, when using the MVC namespace in
   * XML or MVC Java config &mdash; then using this builder might be a good
   * option for testing a majority of controllers. In such cases, a much
   * smaller number of tests can be used to focus on testing and verifying
   * the actual Web MVC configuration.
   *
   * @param controllers one or more {@code @Controller} instances to test
   * (specified {@code Class} will be turned into instance)
   */
  public static StandaloneMockMvcBuilder standaloneSetup(Object... controllers) {
    return new StandaloneMockMvcBuilder(controllers);
  }

}
