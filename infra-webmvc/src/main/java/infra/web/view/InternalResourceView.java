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

package infra.web.view;

import java.util.Map;

import infra.lang.Assert;
import infra.web.RequestContext;

import static infra.util.StringUtils.prependLeadingSlash;

/**
 * Wrapper for other resource within the same web application.
 * Exposes model objects as request attributes and forwards the request to
 * the specified resource URL using a {@link RequestContext#forward(String)}.
 *
 * <p>If operating within an already included request or within a response that
 * has already been committed, this view will fall back to an include instead of
 * a forward. This can be enforced by calling {@code response.flush()}
 * (which will commit the response) before rendering the view.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
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
      String dispatcherPath = prependLeadingSlash(path);
      if (uri.equals(dispatcherPath)) {
        throw new IllegalStateException("Circular view path [" + dispatcherPath + "]: would dispatch back " +
                "to the current handler URL [" + uri + "] again. Check your ViewResolver setup! " +
                "(Hint: This may be the result of an unspecified view, due to default view name generation.)");
      }
    }
    return path;
  }

}