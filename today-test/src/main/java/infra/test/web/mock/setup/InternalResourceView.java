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

package infra.test.web.mock.setup;

import java.util.Map;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.mock.api.MockContext;
import infra.mock.api.MockException;
import infra.mock.api.MockResponse;
import infra.mock.api.RequestDispatcher;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpMockResponse;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.mock.MockContextAware;
import infra.web.mock.MockUtils;
import infra.web.view.AbstractUrlBasedView;

/**
 * Wrapper for a JSP or other resource within the same web application.
 * Exposes model objects as request attributes and forwards the request to
 * the specified resource URL using a {@link RequestDispatcher}.
 *
 * <p>A URL for this view is supposed to specify a resource within the web
 * application, suitable for RequestDispatcher's {@code forward} or
 * {@code include} method.
 *
 * <p>If operating within an already included request or within a response that
 * has already been committed, this view will fall back to an include instead of
 * a forward. This can be enforced by calling {@code response.flushBuffer()}
 * (which will commit the response) before rendering the view.
 *
 * <p>Typical usage with {@link InternalResourceViewResolver} looks as follows,
 * from the perspective of the DispatcherServlet context definition:
 *
 * <pre class="code">&lt;bean id="viewResolver" class="infra.web.mock.view.InternalResourceViewResolver"&gt;
 *   &lt;property name="prefix" value="/WEB-INF/jsp/"/&gt;
 *   &lt;property name="suffix" value=".jsp"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * Every view name returned from a handler will be translated to a JSP
 * resource (for example: "myView" &rarr; "/WEB-INF/jsp/myView.jsp"), using
 * this view class by default.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see RequestDispatcher#forward
 * @see RequestDispatcher#include
 * @see MockResponse#flushBuffer
 * @see InternalResourceViewResolver
 * @since 4.0
 */
public class InternalResourceView extends AbstractUrlBasedView implements MockContextAware {

  @Nullable
  private MockContext mockContext;

  private boolean alwaysInclude = false;

  private boolean preventDispatchLoop = false;

  /**
   * Constructor for use as a bean.
   *
   * @see #setUrl
   * @see #setAlwaysInclude
   */
  public InternalResourceView() { }

  /**
   * Create a new InternalResourceView with the given URL.
   *
   * @param url the URL to forward to
   * @see #setAlwaysInclude
   */
  public InternalResourceView(String url) {
    super(url);
  }

  /**
   * Create a new InternalResourceView with the given URL.
   *
   * @param url the URL to forward to
   * @param alwaysInclude whether to always include the view rather than forward to it
   */
  public InternalResourceView(String url, boolean alwaysInclude) {
    super(url);
    this.alwaysInclude = alwaysInclude;
  }

  /**
   * Specify whether to always include the view rather than forward to it.
   * <p>Default is "false". Switch this flag on to enforce the use of a
   * Servlet include, even if a forward would be possible.
   *
   * @see RequestDispatcher#forward
   * @see RequestDispatcher#include
   * @see #useInclude(HttpMockRequest, HttpMockResponse)
   */
  public void setAlwaysInclude(boolean alwaysInclude) {
    this.alwaysInclude = alwaysInclude;
  }

  /**
   * Set whether to explicitly prevent dispatching back to the
   * current handler path.
   * <p>Default is "false". Switch this to "true" for convention-based
   * views where a dispatch back to the current handler path is a
   * definitive error.
   */
  public void setPreventDispatchLoop(boolean preventDispatchLoop) {
    this.preventDispatchLoop = preventDispatchLoop;
  }

  /**
   * An ApplicationContext is not strictly required for InternalResourceView.
   */
  @Override
  protected boolean isContextRequired() {
    return false;
  }

  /**
   * Render the internal resource given the specified model.
   * This includes setting the model as request attributes.
   */
  @Override
  protected void renderMergedOutputModel(
          Map<String, Object> model, RequestContext request) throws Exception {

    // Expose the model object as request attributes.
    exposeModelAsRequestAttributes(model, request);

    // Determine the path for the request dispatcher.
    HttpMockRequest servletRequest = MockUtils.getMockRequest(request);
    HttpMockResponse servletResponse = MockUtils.getMockResponse(request);

    // Expose helpers as request attributes, if any.
    exposeHelpers(servletRequest, request);

    String dispatcherPath = prepareForRendering(servletRequest, servletResponse);
    // Obtain a RequestDispatcher for the target resource (typically a JSP).
    RequestDispatcher rd = getRequestDispatcher(servletRequest, dispatcherPath);
    if (rd == null) {
      throw new MockException("Could not get RequestDispatcher for [" + getUrl() +
              "]: Check that the corresponding file exists within your web application archive!");
    }

    // If already included or response already committed, perform include, else forward.
    if (useInclude(servletRequest, servletResponse)) {
      request.setContentType(getContentType());
      if (logger.isDebugEnabled()) {
        logger.debug("Including [{}]", getUrl());
      }
      rd.include(servletRequest, servletResponse);
    }
    else {
      // Note: The forwarded resource is supposed to determine the content type itself.
      if (logger.isDebugEnabled()) {
        logger.debug("Forwarding to [{}]", getUrl());
      }
      rd.forward(servletRequest, servletResponse);
    }
  }

  /**
   * Expose helpers unique to each rendering operation. This is necessary so that
   * different rendering operations can't overwrite each other's contexts etc.
   * <p>Called by {@link #renderMergedOutputModel(Map, RequestContext)}.
   * The default implementation is empty. This method can be overridden to add
   * custom helpers as request attributes.
   *
   * @param request current HTTP request
   * @throws Exception if there's a fatal error while we're adding attributes
   * @see #renderMergedOutputModel
   * @see InternalResourceView#exposeHelpers
   */
  protected void exposeHelpers(HttpMockRequest servletRequest, RequestContext request) throws Exception { }

  /**
   * Prepare for rendering, and determine the request dispatcher path
   * to forward to (or to include).
   * <p>This implementation simply returns the configured URL.
   * Subclasses can override this to determine a resource to render,
   * typically interpreting the URL in a different manner.
   *
   * @param request current HTTP request
   * @param response current HTTP response
   * @return the request dispatcher path to use
   * @throws Exception if preparations failed
   * @see #getUrl()
   */
  protected String prepareForRendering(HttpMockRequest request, HttpMockResponse response)
          throws Exception {

    String path = getUrl();
    Assert.state(path != null, "'url' not set");

    if (this.preventDispatchLoop) {
      String uri = request.getRequestURI();
      if (path.startsWith("/") ? uri.equals(path) : uri.equals(StringUtils.applyRelativePath(uri, path))) {
        throw new MockException("Circular view path [" + path + "]: would dispatch back " +
                "to the current handler URL [" + uri + "] again. Check your ViewResolver setup! " +
                "(Hint: This may be the result of an unspecified view, due to default view name generation.)");
      }
    }
    return path;
  }

  /**
   * Obtain the RequestDispatcher to use for the forward/include.
   * <p>The default implementation simply calls
   * {@link HttpMockRequest#getRequestDispatcher(String)}.
   * Can be overridden in subclasses.
   *
   * @param request current HTTP request
   * @param path the target URL (as returned from {@link #prepareForRendering})
   * @return a corresponding RequestDispatcher
   */
  @Nullable
  protected RequestDispatcher getRequestDispatcher(HttpMockRequest request, String path) {
    return request.getRequestDispatcher(path);
  }

  /**
   * Determine whether to use RequestDispatcher's {@code include} or
   * {@code forward} method.
   * <p>Performs a check whether an include URI attribute is found in the request,
   * indicating an include request, and whether the response has already been committed.
   * In both cases, an include will be performed, as a forward is not possible anymore.
   *
   * @param request current HTTP request
   * @param response current HTTP response
   * @return {@code true} for include, {@code false} for forward
   * @see RequestDispatcher#forward
   * @see RequestDispatcher#include
   * @see MockResponse#isCommitted
   * @see MockUtils#isIncludeRequest
   */
  protected boolean useInclude(HttpMockRequest request, HttpMockResponse response) {
    return alwaysInclude || MockUtils.isIncludeRequest(request) || response.isCommitted();
  }

  @Override
  public final void setMockContext(MockContext mockContext) {
    if (mockContext != this.mockContext) {
      initMockContext(this.mockContext = mockContext);
    }
  }

  protected void initMockContext(MockContext mockContext) { }

  @Nullable
  public MockContext getMockContext() {
    return mockContext;
  }
}