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

package infra.test.web.mock;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import infra.beans.Mergeable;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.mock.api.AsyncContext;
import infra.mock.api.DispatcherType;
import infra.mock.api.Filter;
import infra.mock.api.MockContext;
import infra.mock.api.MockResponse;
import infra.mock.api.http.HttpMockResponse;
import infra.mock.api.http.HttpMockResponseWrapper;
import infra.mock.web.MockFilterChain;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.mock.MockDispatcher;
import infra.web.mock.MockRequestContext;
import infra.test.web.mock.request.MockMvcRequestBuilders;
import infra.test.web.mock.result.MockMvcResultMatchers;
import infra.test.web.mock.setup.ConfigurableMockMvcBuilder;
import infra.test.web.mock.setup.DefaultMockMvcBuilder;
import infra.test.web.mock.setup.MockMvcBuilders;

/**
 * <strong>Main entry point for server-side Web MVC test support.</strong>
 *
 * <h3>Example</h3>
 *
 * <pre class="code">
 * import static infra.test.web.servlet.request.MockMvcRequestBuilders.*;
 * import static infra.test.web.servlet.result.MockMvcResultMatchers.*;
 * import static infra.test.web.servlet.setup.MockMvcBuilders.*;
 *
 * // ...
 *
 * WebApplicationContext wac = ...;
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

  private final TestMockDispatcher mock;

  private final Filter[] filters;

  private final MockContext mockContext;

  @Nullable
  private RequestBuilder defaultRequestBuilder;

  @Nullable
  private Charset defaultResponseCharacterEncoding;

  private List<ResultMatcher> defaultResultMatchers = new ArrayList<>();

  private List<ResultHandler> defaultResultHandlers = new ArrayList<>();

  /**
   * Private constructor, not for direct instantiation.
   *
   * @see MockMvcBuilders
   */
  MockMvc(TestMockDispatcher mock, Filter... filters) {
    Assert.notNull(mock, "DispatcherHandler is required");
    Assert.notNull(filters, "Filters cannot be null");
    Assert.noNullElements(filters, "Filters cannot contain null values");

    this.mock = mock;
    this.filters = filters;
    this.mockContext = mock.getMockContext();
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
   * Return the underlying {@link MockDispatcher} instance that this
   * {@code MockMvc} was initialized with.
   * <p>This is intended for use in custom request processing scenario where a
   * request handling component happens to delegate to the {@code DispatcherServlet}
   * at runtime and therefore needs to be injected with it.
   * <p>For most processing scenarios, simply use {@link MockMvc#perform},
   * or if you need to configure the {@code DispatcherServlet}, provide a
   * {@link DispatcherCustomizer} to the {@code MockMvcBuilder}.
   */
  public MockDispatcher getDispatcher() {
    return this.mock;
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

    RequestContext previous = RequestContextHolder.get();

    var context = new MockRequestContext(mock.getApplicationContext(), request, servletResponse, mock);
    DefaultMvcResult mvcResult = new DefaultMvcResult(request, mockResponse, context);

    RequestContextHolder.set(context);

    MockFilterChain filterChain = new MockFilterChain(this.mock, this.filters);
    filterChain.doFilter(request, servletResponse);

    RequestContext maybeNew = RequestContextHolder.getRequired();
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
