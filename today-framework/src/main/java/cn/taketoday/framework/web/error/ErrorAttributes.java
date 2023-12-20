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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.error;

import java.util.Collections;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.view.ModelAndView;

/**
 * Provides access to error attributes which can be logged or presented to the user.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultErrorAttributes
 * @since 4.0
 */
public interface ErrorAttributes {

  /**
   * Returns a {@link Map} of the error attributes. The map can be used as the model of
   * an error page {@link ModelAndView}, or returned as a
   * {@link ResponseBody @ResponseBody}.
   *
   * @param context the source request
   * @param options options for error attribute contents
   * @return a map of error attributes
   */
  default Map<String, Object> getErrorAttributes(RequestContext context, ErrorAttributeOptions options) {
    return Collections.emptyMap();
  }

  /**
   * Return the underlying cause of the error or {@code null} if the error cannot be
   * extracted.
   *
   * @param webRequest the source request
   * @return the {@link Exception} that caused the error or {@code null}
   */
  @Nullable
  Throwable getError(RequestContext webRequest);

}
