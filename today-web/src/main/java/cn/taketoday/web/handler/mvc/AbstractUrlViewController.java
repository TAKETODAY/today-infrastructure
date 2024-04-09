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

package cn.taketoday.web.handler.mvc;

import cn.taketoday.web.RedirectModel;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.ModelAndView;

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
