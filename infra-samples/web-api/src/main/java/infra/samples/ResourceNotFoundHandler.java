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

package infra.samples;

import infra.http.HttpStatus;
import infra.lang.Nullable;
import infra.stereotype.Component;
import infra.web.NotFoundHandler;
import infra.web.RequestContext;
import infra.web.handler.SimpleNotFoundHandler;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NotFoundHandler
 * @since 2020-01-01 22:29
 */
@Component
public class ResourceNotFoundHandler implements NotFoundHandler {

  @Nullable
  @Override
  public Object handleNotFound(RequestContext request) throws Throwable {
    request.setStatus(HttpStatus.NOT_FOUND);

    SimpleNotFoundHandler.logNotFound(request);
    return ErrorMessage.failed("资源找不到");
  }

}
