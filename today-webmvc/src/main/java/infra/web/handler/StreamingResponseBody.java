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

package infra.web.handler;

import java.io.IOException;
import java.io.OutputStream;

import infra.web.handler.method.RequestMappingHandlerAdapter;

/**
 * A http request handler return value type for asynchronous request processing
 * where the application can write directly to the response {@code OutputStream}
 * without holding up the container thread.
 *
 * <p><strong>Note:</strong> when using this option it is highly recommended to
 * configure explicitly the TaskExecutor used in Web MVC for executing
 * asynchronous requests. Both the MVC Java config and the MVC namespaces provide
 * options to configure asynchronous handling. If not using those, an application
 * can set the {@code taskExecutor} property of
 * {@link RequestMappingHandlerAdapter RequestMappingHandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/9 12:07
 */
@FunctionalInterface
public interface StreamingResponseBody {

  /**
   * A callback for writing to the response body.
   *
   * @param outputStream the stream for the response body
   * @throws IOException an exception while writing
   */
  void writeTo(OutputStream outputStream) throws IOException;

}
