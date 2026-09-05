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

import infra.beans.Mergeable;
import infra.context.ApplicationContext;
import infra.test.web.mock.request.MockMvcRequestBuilders;
import infra.test.web.mock.result.MockMvcResultMatchers;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.util.Assert;
import infra.web.Filter;
import infra.web.HttpContext;
import infra.web.HttpContextHolder;
import infra.web.mock.MockDispatcherHandler;
import infra.web.mock.MockFilterChain;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.mock.api.AsyncContext;
import infra.web.mock.api.DispatcherType;
import infra.web.mock.api.MockContext;

/**
 * <strong>Main entry point for server-side Web MVC test support.</strong>
 *
 * <h3>Example</h3>
 *
 * <pre class="code">
 * import static infra.test.web.mock.request.MockMvcRequestBuilders.*;
 * import static infra.test.web.mock.result.MockMvcResultMatchers.*;
 * import static infra.test.web.mock.setup.MockMvcBuilders.*;
 *
 * // ...
 *
 * ApplicationContext wac = ...;
 *
 * MockMvc mockMvc = webAppContextSetup(wac).build();
 *
 * mockMvc.perform(get("/form"))
 *     .andExpectAll(
 *         status().isOk(),
 *         content().contentType("text/html"),
 *         forwardedUrl("/WEB-INF/layouts/main.jsp")
 *     );
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.0
 */
public final class MockMvc {

  private final Filter[] filters;

  private final MockContext mockContext;

  private final List<ResultMatcher> defaultResultMatchers;

  private final List<ResultHandler> defaultResultHandlers;

  private final @Nullable RequestBuilder defaultRequestBuilder;

  private final @Nullable Charset defaultResponseCharacterEncoding;

  /**
   * Private constructor, not for direct instantiation.
   *
   * @see MockMvcBuilders
   */
  MockMvc(MockContext mockContext, Filter[] filters, @Nullable RequestBuilder defaultRequestBuilder,
          List<ResultMatcher> defaultResultMatchers, List<ResultHandler> defaultResultHandlers,
          @Nullable Charset defaultResponseCharacterEncoding) {
    Assert.notNull(filters, "Filters cannot be null");
    Assert.notNull(mockContext, "MockContext is required");
    Assert.notNull(mockContext.getDispatcherHandler(), "DispatcherHandler is required");
    Assert.noNullElements(filters, "Filters cannot contain null values");

    this.defaultResponseCharacterEncoding = defaultResponseCharacterEncoding;
    this.defaultResultMatchers = defaultResultMatchers;
    this.defaultResultHandlers = defaultResultHandlers;
    this.defaultRequestBuilder = defaultRequestBuilder;
    this.mockContext = mockContext;
    this.filters = filters;
  }

  /**
   * Return the underlying {@link MockContext} that this {@code MockMvc} was
   * initialized with, exposing the associated {@link ApplicationContext} and
   * {@link MockDispatcherHandler dispatcher handler}.
   *
   * @return the associated mock context, never {@code null}
   */
  public MockContext getMockContext() {
    return mockContext;
  }

  /**
   * Perform a request and return a type that allows chaining further
   * actions, such as asserting expectations, on the result.
   *
   * @param requestBuilder used to prepare the request to execute;
   * see static factory methods in
   * {@link MockMvcRequestBuilders}
   * @return an instance of {@link ResultActions} (never {@code null})
   * @see MockMvcRequestBuilders
   * @see MockMvcResultMatchers
   */
  public ResultActions perform(RequestBuilder requestBuilder) throws Exception {
    if (defaultRequestBuilder != null && requestBuilder instanceof Mergeable mergeable) {
      requestBuilder = (RequestBuilder) mergeable.merge(defaultRequestBuilder);
    }

    MockRequest request = requestBuilder.buildRequest(mockContext);

    AsyncContext asyncContext = request.getAsyncContext();
    MockResponse response;
    if (asyncContext != null) {
      response = asyncContext.getResponse();
    }
    else {
      response = new MockResponse();
    }

    if (this.defaultResponseCharacterEncoding != null) {
      response.setDefaultCharacterEncoding(this.defaultResponseCharacterEncoding.name());
    }

    HttpContext previous = HttpContextHolder.current();

    var context = new MockHttpContext(mockContext.getApplicationContext(),
            request, response, mockContext.getDispatcherHandler());
    DefaultMvcResult mvcResult = new DefaultMvcResult(request, response, context);

    if (requestBuilder instanceof SmartRequestBuilder smartRequestBuilder) {
      smartRequestBuilder.customize(request, context);
    }

    HttpContextHolder.set(context);

    MockFilterChain filterChain = new MockFilterChain(mockContext.getDispatcherHandler(), this.filters);
    filterChain.doFilter(context);

    HttpContext maybeNew = HttpContextHolder.required();
    if (maybeNew != context) {
      mvcResult.setContext(maybeNew);
    }

    if (DispatcherType.ASYNC.equals(request.getDispatcherType())
            && asyncContext != null && !request.isAsyncStarted()) {
      asyncContext.complete();
    }

    applyDefaultResultActions(mvcResult);
    HttpContextHolder.set(previous);
    return ResultActions.forMvcResult(mvcResult);
  }

  private void applyDefaultResultActions(MvcResult mvcResult) throws Exception {
    for (ResultHandler handler : this.defaultResultHandlers) {
      handler.handle(mvcResult);
    }
    for (ResultMatcher matcher : this.defaultResultMatchers) {
      matcher.match(mvcResult);
    }
  }

}
