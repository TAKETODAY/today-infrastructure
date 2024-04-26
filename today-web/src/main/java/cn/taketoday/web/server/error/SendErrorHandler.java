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

package cn.taketoday.web.server.error;

import java.io.IOException;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestContext#sendError
 * @since 4.0 2023/7/27 18:01
 */
public interface SendErrorHandler {

  /**
   * handle {@link RequestContext#sendError} methods invocation
   *
   * @param request current HTTP request
   * @param message error message
   * @throws IOException send errors
   */
  void handleError(RequestContext request, @Nullable String message) throws IOException;
}
