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

import infra.test.web.mock.setup.DefaultMockMvcBuilder;
import infra.web.Filter;
import infra.web.mock.api.MockContext;
import infra.web.mock.support.GenericMockWebApplicationContext;

/**
 * Base class for MockMvc builder implementations, providing the capability to
 * create a {@link MockMvc} instance.
 *
 * <p>{@link DefaultMockMvcBuilder},
 * which derives from this class, provides a concrete {@code build} method,
 * and delegates to abstract methods to obtain a {@link GenericMockWebApplicationContext}.
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
   * Create a {@link MockMvc} instance from the supplied configuration.
   *
   * @param filters the filters to apply to every performed request
   * @param mockContext the mock context that carries the application context and
   * the dispatcher handler
   * @param defaultRequestBuilder the default request builder merged into every
   * performed request, or {@code null}
   * @param defaultResponseCharacterEncoding the default character encoding applied
   * to every response, or {@code null}
   * @param globalResultMatchers the expectations to assert after every performed
   * request
   * @param globalResultHandlers the general actions to apply after every performed
   * request
   * @return the created {@link MockMvc} instance
   */
  protected final MockMvc createMockMvc(Filter[] filters, MockContext mockContext,
          @Nullable RequestBuilder defaultRequestBuilder, @Nullable Charset defaultResponseCharacterEncoding,
          List<ResultMatcher> globalResultMatchers, List<ResultHandler> globalResultHandlers) {

    return new MockMvc(mockContext, filters, defaultRequestBuilder,
            globalResultMatchers, globalResultHandlers, defaultResponseCharacterEncoding);
  }

}
