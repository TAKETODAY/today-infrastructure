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

package cn.taketoday.test.web.servlet.setup;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.MockMvcBuilder;
import cn.taketoday.web.mock.WebApplicationContext;

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
   * <p>The {@link cn.taketoday.web.mock.DispatcherServlet DispatcherServlet}
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
   * {@link cn.taketoday.web.mock.DispatcherServlet DispatcherServlet}
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
