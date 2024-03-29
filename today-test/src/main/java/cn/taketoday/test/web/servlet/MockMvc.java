/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.web.servlet;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.Mergeable;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockFilterChain;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.ServletRequestContext;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * <strong>Main entry point for server-side Web MVC test support.</strong>
 *
 * <h3>Example</h3>
 *
 * <pre class="code">
 * import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.*;
 * import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.*;
 * import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.*;
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

  private final TestDispatcherServlet servlet;

  private final Filter[] filters;

  private final ServletContext servletContext;

  @Nullable
  private RequestBuilder defaultRequestBuilder;

  @Nullable
  private Charset defaultResponseCharacterEncoding;

  private List<ResultMatcher> defaultResultMatchers = new ArrayList<>();

  private List<ResultHandler> defaultResultHandlers = new ArrayList<>();

  /**
   * Private constructor, not for direct instantiation.
   *
   * @see cn.taketoday.test.web.servlet.setup.MockMvcBuilders
   */
  MockMvc(TestDispatcherServlet servlet, Filter... filters) {
    Assert.notNull(servlet, "DispatcherServlet is required");
    Assert.notNull(filters, "Filters cannot be null");
    Assert.noNullElements(filters, "Filters cannot contain null values");

    this.servlet = servlet;
    this.filters = filters;
    this.servletContext = servlet.getServletContext();
  }

  /**
   * A default request builder merged into every performed request.
   *
   * @see cn.taketoday.test.web.servlet.setup.DefaultMockMvcBuilder#defaultRequest(RequestBuilder)
   */
  void setDefaultRequest(@Nullable RequestBuilder requestBuilder) {
    this.defaultRequestBuilder = requestBuilder;
  }

  /**
   * The default character encoding to be applied to every response.
   *
   * @see cn.taketoday.test.web.servlet.setup.ConfigurableMockMvcBuilder#defaultResponseCharacterEncoding(Charset)
   */
  void setDefaultResponseCharacterEncoding(@Nullable Charset defaultResponseCharacterEncoding) {
    this.defaultResponseCharacterEncoding = defaultResponseCharacterEncoding;
  }

  /**
   * Expectations to assert after every performed request.
   *
   * @see cn.taketoday.test.web.servlet.setup.DefaultMockMvcBuilder#alwaysExpect(ResultMatcher)
   */
  void setGlobalResultMatchers(List<ResultMatcher> resultMatchers) {
    Assert.notNull(resultMatchers, "ResultMatcher List is required");
    this.defaultResultMatchers = resultMatchers;
  }

  /**
   * General actions to apply after every performed request.
   *
   * @see cn.taketoday.test.web.servlet.setup.DefaultMockMvcBuilder#alwaysDo(ResultHandler)
   */
  void setGlobalResultHandlers(List<ResultHandler> resultHandlers) {
    Assert.notNull(resultHandlers, "ResultHandler List is required");
    this.defaultResultHandlers = resultHandlers;
  }

  /**
   * Return the underlying {@link DispatcherServlet} instance that this
   * {@code MockMvc} was initialized with.
   * <p>This is intended for use in custom request processing scenario where a
   * request handling component happens to delegate to the {@code DispatcherServlet}
   * at runtime and therefore needs to be injected with it.
   * <p>For most processing scenarios, simply use {@link MockMvc#perform},
   * or if you need to configure the {@code DispatcherServlet}, provide a
   * {@link DispatcherServletCustomizer} to the {@code MockMvcBuilder}.
   */
  public DispatcherServlet getDispatcherServlet() {
    return this.servlet;
  }

  /**
   * Perform a request and return a type that allows chaining further
   * actions, such as asserting expectations, on the result.
   *
   * @param requestBuilder used to prepare the request to execute;
   * see static factory methods in
   * {@link cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders}
   * @return an instance of {@link ResultActions} (never {@code null})
   * @see cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders
   * @see cn.taketoday.test.web.servlet.result.MockMvcResultMatchers
   */
  public ResultActions perform(RequestBuilder requestBuilder) throws Exception {
    if (this.defaultRequestBuilder != null && requestBuilder instanceof Mergeable mergeable) {
      requestBuilder = (RequestBuilder) mergeable.merge(this.defaultRequestBuilder);
    }

    MockHttpServletRequest request = requestBuilder.buildRequest(this.servletContext);

    AsyncContext asyncContext = request.getAsyncContext();
    MockHttpServletResponse mockResponse;
    HttpServletResponse servletResponse;
    if (asyncContext != null) {
      servletResponse = (HttpServletResponse) asyncContext.getResponse();
      mockResponse = unwrapResponseIfNecessary(servletResponse);
    }
    else {
      mockResponse = new MockHttpServletResponse();
      servletResponse = mockResponse;
    }

    if (this.defaultResponseCharacterEncoding != null) {
      mockResponse.setDefaultCharacterEncoding(this.defaultResponseCharacterEncoding.name());
    }

    if (requestBuilder instanceof SmartRequestBuilder smartRequestBuilder) {
      request = smartRequestBuilder.postProcessRequest(request);
    }

    RequestContext previous = RequestContextHolder.get();

    var context = new ServletRequestContext(servlet.getApplicationContext(), request, servletResponse, servlet);
    DefaultMvcResult mvcResult = new DefaultMvcResult(request, mockResponse, context);

    RequestContextHolder.set(context);

    MockFilterChain filterChain = new MockFilterChain(this.servlet, this.filters);
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

  private MockHttpServletResponse unwrapResponseIfNecessary(ServletResponse servletResponse) {
    while (servletResponse instanceof HttpServletResponseWrapper wrapper) {
      servletResponse = wrapper.getResponse();
    }
    Assert.isInstanceOf(MockHttpServletResponse.class, servletResponse);
    return (MockHttpServletResponse) servletResponse;
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
