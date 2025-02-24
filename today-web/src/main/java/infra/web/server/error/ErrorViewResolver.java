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

package infra.web.server.error;

import java.util.Map;

import infra.http.HttpStatusCode;
import infra.lang.Nullable;
import infra.web.RequestContext;
import infra.web.view.ModelAndView;

/**
 * Interface that can be implemented by beans that resolve error views.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface ErrorViewResolver {

  /**
   * Resolve an error view for the specified details.
   *
   * @param request the source request
   * @param status the http status of the error
   * @param model the suggested model to be used with the view
   * @return a resolved {@link ModelAndView} or {@code null}
   */
  @Nullable
  ModelAndView resolveErrorView(RequestContext request, HttpStatusCode status, Map<String, Object> model);

}
