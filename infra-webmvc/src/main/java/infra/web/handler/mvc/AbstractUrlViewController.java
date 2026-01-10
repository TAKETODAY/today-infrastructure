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

package infra.web.handler.mvc;

import infra.web.RedirectModel;
import infra.web.RequestContext;
import infra.web.view.ModelAndView;

/**
 * Abstract base class for {@code Controllers} that return a view name
 * based on the request URL.
 *
 * <p>Provides infrastructure for determining view names from URLs and configurable
 * URL lookup. For information on the latter, see {@code alwaysUseFullPath}
 * and {@code urlDecode} properties.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 16:49
 */
public abstract class AbstractUrlViewController extends AbstractController {

  /**
   * Retrieves the URL path to use for lookup and delegates to
   * {@link #getViewNameForRequest}. Also adds the content of
   * {@link RequestContext#getInputRedirectModel()} to the model.
   */
  @Override
  protected ModelAndView handleRequestInternal(RequestContext request) {
    String viewName = getViewNameForRequest(request);
    if (logger.isTraceEnabled()) {
      logger.trace("Returning view name '{}'", viewName);
    }
    RedirectModel model = request.getInputRedirectModel(null);
    if (model != null) {
      return new ModelAndView(viewName, model.asMap());
    }
    return new ModelAndView(viewName);
  }

  /**
   * Return the name of the view to render for this request, based on the
   * given lookup path. Called by {@link #handleRequestInternal}.
   *
   * @param request current HTTP request
   * @return a view name for this request (never {@code null})
   * @see #handleRequestInternal
   */
  protected abstract String getViewNameForRequest(RequestContext request);

}
