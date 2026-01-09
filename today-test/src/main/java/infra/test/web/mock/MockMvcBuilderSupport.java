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
