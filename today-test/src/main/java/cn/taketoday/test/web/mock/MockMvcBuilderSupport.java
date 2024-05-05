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

package cn.taketoday.test.web.mock;

import java.nio.charset.Charset;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.Filter;
import cn.taketoday.mock.web.MockMockConfig;
import cn.taketoday.web.mock.WebApplicationContext;

/**
 * Base class for MockMvc builder implementations, providing the capability to
 * create a {@link MockMvc} instance.
 *
 * <p>{@link cn.taketoday.test.web.mock.setup.DefaultMockMvcBuilder},
 * which derives from this class, provides a concrete {@code build} method,
 * and delegates to abstract methods to obtain a {@link WebApplicationContext}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class MockMvcBuilderSupport {

  /**
   * Delegates to {@link #createMockMvc(Filter[], MockMockConfig, ApplicationContext, RequestBuilder, List, List, List)}
   * for creation of the {@link MockMvc} instance and configures that instance
   * with the supplied {@code defaultResponseCharacterEncoding}.
   */
  protected final MockMvc createMockMvc(Filter[] filters, MockMockConfig servletConfig,
          ApplicationContext webAppContext, @Nullable RequestBuilder defaultRequestBuilder,
          @Nullable Charset defaultResponseCharacterEncoding,
          List<ResultMatcher> globalResultMatchers, List<ResultHandler> globalResultHandlers,
          @Nullable List<DispatcherServletCustomizer> dispatcherServletCustomizers) {

    MockMvc mockMvc = createMockMvc(filters, servletConfig, webAppContext, defaultRequestBuilder,
            globalResultMatchers, globalResultHandlers, dispatcherServletCustomizers);

    mockMvc.setDefaultResponseCharacterEncoding(defaultResponseCharacterEncoding);
    return mockMvc;
  }

  protected final MockMvc createMockMvc(Filter[] filters, MockMockConfig servletConfig,
          ApplicationContext webAppContext, @Nullable RequestBuilder defaultRequestBuilder,
          List<ResultMatcher> globalResultMatchers, List<ResultHandler> globalResultHandlers,
          @Nullable List<DispatcherServletCustomizer> dispatcherServletCustomizers) {

    TestDispatcherServlet dispatcherServlet = new TestDispatcherServlet(webAppContext);
    if (dispatcherServletCustomizers != null) {
      for (DispatcherServletCustomizer customizers : dispatcherServletCustomizers) {
        customizers.customize(dispatcherServlet);
      }
    }

    dispatcherServlet.init(servletConfig);

    MockMvc mockMvc = new MockMvc(dispatcherServlet, filters);
    mockMvc.setDefaultRequest(defaultRequestBuilder);
    mockMvc.setGlobalResultMatchers(globalResultMatchers);
    mockMvc.setGlobalResultHandlers(globalResultHandlers);

    return mockMvc;
  }

}
