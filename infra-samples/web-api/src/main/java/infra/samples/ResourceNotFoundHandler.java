/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.samples;

import infra.http.HttpStatus;
import org.jspecify.annotations.Nullable;
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
class ResourceNotFoundHandler implements NotFoundHandler {

  @Nullable
  @Override
  public Object handleNotFound(RequestContext request) throws Throwable {
    request.setStatus(HttpStatus.NOT_FOUND);

    SimpleNotFoundHandler.logNotFound(request);
    return ErrorMessage.failed("资源找不到");
  }

}
