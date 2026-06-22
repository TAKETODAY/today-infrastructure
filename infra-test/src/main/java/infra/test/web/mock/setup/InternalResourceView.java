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

package infra.test.web.mock.setup;

import java.util.Map;

import infra.lang.Assert;
import infra.mock.api.MockException;
import infra.mock.api.MockResponse;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.view.AbstractUrlBasedView;

/**
 * Wrapper for a JSP or other resource within the same web application.
 * Exposes model objects as request attributes and forwards the request to
 * the specified resource URL using a {@link RequestContext#forward(String)}.
 *
 * <p>If operating within an already included request or within a response that
 * has already been committed, this view will fall back to an include instead of
 * a forward. This can be enforced by calling {@code response.flushBuffer()}
 * (which will commit the response) before rendering the view.
 *
 * <p>Typical usage with {@link InternalResourceViewResolver} looks as follows,
 * from the perspective of the DispatcherHandler context definition:
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
 * @see MockResponse#flushBuffer
 * @see InternalResourceViewResolver
 * @since 4.0
 */
public class InternalResourceView extends AbstractUrlBasedView {

  private boolean preventDispatchLoop = false;

  /**
   * Constructor for use as a bean.
   *
   * @see #setUrl
   */
  public InternalResourceView() {
  }

  /**
   * Create a new InternalResourceView with the given URL.
   *
   * @param url the URL to forward to
   */
  public InternalResourceView(String url) {
    super(url);
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
  protected void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws Exception {
    // Expose the model object as request attributes.
    exposeModelAsRequestAttributes(model, request);

    // Expose helpers as request attributes, if any.
    exposeHelpers(request);

    String dispatcherPath = prepareForRendering(request);
    request.forward(dispatcherPath);
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
  protected void exposeHelpers(RequestContext request) throws Exception {
  }

  /**
   * Prepare for rendering, and determine the request dispatcher path
   * to forward to (or to include).
   * <p>This implementation simply returns the configured URL.
   * Subclasses can override this to determine a resource to render,
   * typically interpreting the URL in a different manner.
   *
   * @param request current HTTP request
   * @return the request dispatcher path to use
   * @throws Exception if preparations failed
   * @see #getUrl()
   */
  protected String prepareForRendering(RequestContext request) throws Exception {

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

}