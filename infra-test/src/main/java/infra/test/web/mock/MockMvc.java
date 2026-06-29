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
import java.util.ArrayList;
import java.util.List;

import infra.beans.Mergeable;
import infra.lang.Assert;
import infra.mock.api.AsyncContext;
import infra.mock.api.DispatcherType;
import infra.mock.api.MockContext;
import infra.mock.api.MockResponse;
import infra.mock.api.http.HttpMockResponse;
import infra.mock.api.http.HttpMockResponseWrapper;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockFilterChain;
import infra.mock.web.MockHttpResponseImpl;
import infra.test.web.mock.request.MockMvcRequestBuilders;
import infra.test.web.mock.result.MockMvcResultMatchers;
import infra.test.web.mock.setup.ConfigurableMockMvcBuilder;
import infra.test.web.mock.setup.DefaultMockMvcBuilder;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.web.Filter;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.mock.MockDispatcherHandler;
import infra.web.mock.MockRequestContext;

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

  private final TestMockDispatcherHandler dispatcherHandler;

  private final Filter[] filters;

  private final MockContext mockContext;

  private @Nullable RequestBuilder defaultRequestBuilder;

  private @Nullable Charset defaultResponseCharacterEncoding;

  private List<ResultMatcher> defaultResultMatchers = new ArrayList<>();

  private List<ResultHandler> defaultResultHandlers = new ArrayList<>();

  /**
   * Private constructor, not for direct instantiation.
   *
   * @see MockMvcBuilders
   */
  MockMvc(TestMockDispatcherHandler dispatcherHandler, MockContext mockContext, Filter... filters) {
    Assert.notNull(dispatcherHandler, "DispatcherHandler is required");
    Assert.notNull(filters, "Filters cannot be null");
    Assert.noNullElements(filters, "Filters cannot contain null values");

    this.dispatcherHandler = dispatcherHandler;
    this.filters = filters;
    this.mockContext = mockContext;
  }

  /**
   * A default request builder merged into every performed request.
   *
   * @see DefaultMockMvcBuilder#defaultRequest(RequestBuilder)
   */
  void setDefaultRequest(@Nullable RequestBuilder requestBuilder) {
    this.defaultRequestBuilder = requestBuilder;
  }

  /**
   * The default character encoding to be applied to every response.
   *
   * @see ConfigurableMockMvcBuilder#defaultResponseCharacterEncoding(Charset)
   */
  void setDefaultResponseCharacterEncoding(@Nullable Charset defaultResponseCharacterEncoding) {
    this.defaultResponseCharacterEncoding = defaultResponseCharacterEncoding;
  }

  /**
   * Expectations to assert after every performed request.
   *
   * @see DefaultMockMvcBuilder#alwaysExpect(ResultMatcher)
   */
  void setGlobalResultMatchers(List<ResultMatcher> resultMatchers) {
    Assert.notNull(resultMatchers, "ResultMatcher List is required");
    this.defaultResultMatchers = resultMatchers;
  }

  /**
   * General actions to apply after every performed request.
   *
   * @see DefaultMockMvcBuilder#alwaysDo(ResultHandler)
   */
  void setGlobalResultHandlers(List<ResultHandler> resultHandlers) {
    Assert.notNull(resultHandlers, "ResultHandler List is required");
    this.defaultResultHandlers = resultHandlers;
  }

  /**
   * Return the underlying {@link MockDispatcherHandler} instance that this
   * {@code MockMvc} was initialized with.
   * <p>This is intended for use in custom request processing scenario where a
   * request handling component happens to delegate to the {@code Dispatcher}
   * at runtime and therefore needs to be injected with it.
   * <p>For most processing scenarios, simply use {@link MockMvc#perform},
   * or if you need to configure the {@code DispatcherHandler}, provide a
   * {@link DispatcherCustomizer} to the {@code MockMvcBuilder}.
   */
  public MockDispatcherHandler getDispatcher() {
    return this.dispatcherHandler;
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
    if (this.defaultRequestBuilder != null && requestBuilder instanceof Mergeable mergeable) {
      requestBuilder = (RequestBuilder) mergeable.merge(this.defaultRequestBuilder);
    }

    HttpMockRequestImpl request = requestBuilder.buildRequest(this.mockContext);

    AsyncContext asyncContext = request.getAsyncContext();
    MockHttpResponseImpl mockResponse;
    HttpMockResponse servletResponse;
    if (asyncContext != null) {
      servletResponse = (HttpMockResponse) asyncContext.getResponse();
      mockResponse = unwrapResponseIfNecessary(servletResponse);
    }
    else {
      mockResponse = new MockHttpResponseImpl();
      servletResponse = mockResponse;
    }

    if (this.defaultResponseCharacterEncoding != null) {
      mockResponse.setDefaultCharacterEncoding(this.defaultResponseCharacterEncoding.name());
    }

    if (requestBuilder instanceof SmartRequestBuilder smartRequestBuilder) {
      request = smartRequestBuilder.postProcessRequest(request);
    }

    RequestContext previous = RequestContextHolder.current();

    var context = new MockRequestContext(dispatcherHandler.getApplicationContext(), request, servletResponse, dispatcherHandler);
    DefaultMvcResult mvcResult = new DefaultMvcResult(request, mockResponse, context);

    RequestContextHolder.set(context);

    MockFilterChain filterChain = new MockFilterChain(this.dispatcherHandler, this.filters);
    filterChain.doFilter(context);

    RequestContext maybeNew = RequestContextHolder.required();
    if (maybeNew != context) {
      mvcResult.setRequestContext(maybeNew);
    }

    if (DispatcherType.ASYNC.equals(request.getDispatcherType()) &&
            asyncContext != null && !request.isAsyncStarted()) {
      asyncContext.complete();
    }

    applyDefaultResultActions(mvcResult);
    RequestContextHolder.set(previous);
    return ResultActions.forMvcResult(mvcResult);
  }

  private MockHttpResponseImpl unwrapResponseIfNecessary(MockResponse mockResponse) {
    while (mockResponse instanceof HttpMockResponseWrapper wrapper) {
      mockResponse = wrapper.getResponse();
    }
    Assert.isInstanceOf(MockHttpResponseImpl.class, mockResponse);
    return (MockHttpResponseImpl) mockResponse;
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
