/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.test.web.mock;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.List;

import infra.context.ApplicationContext;
import infra.mock.api.Filter;
import infra.mock.web.MockMockConfig;
import infra.test.web.mock.setup.DefaultMockMvcBuilder;
import infra.web.mock.WebApplicationContext;

/**
 * Base class for MockMvc builder implementations, providing the capability to
 * create a {@link MockMvc} instance.
 *
 * <p>{@link DefaultMockMvcBuilder},
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
          @Nullable List<DispatcherCustomizer> dispatcherCustomizers) {

    MockMvc mockMvc = createMockMvc(filters, servletConfig, webAppContext, defaultRequestBuilder,
            globalResultMatchers, globalResultHandlers, dispatcherCustomizers);

    mockMvc.setDefaultResponseCharacterEncoding(defaultResponseCharacterEncoding);
    return mockMvc;
  }

  protected final MockMvc createMockMvc(Filter[] filters, MockMockConfig servletConfig,
          ApplicationContext webAppContext, @Nullable RequestBuilder defaultRequestBuilder,
          List<ResultMatcher> globalResultMatchers, List<ResultHandler> globalResultHandlers,
          @Nullable List<DispatcherCustomizer> dispatcherCustomizers) {

    TestMockDispatcher dispatcherServlet = new TestMockDispatcher(webAppContext);
    if (dispatcherCustomizers != null) {
      for (DispatcherCustomizer customizers : dispatcherCustomizers) {
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
