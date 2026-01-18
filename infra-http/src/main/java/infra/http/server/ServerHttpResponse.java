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

package infra.http.server;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;

import infra.http.HttpOutputMessage;
import infra.http.HttpStatusCode;

/**
 * Represents a server-side HTTP response.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0
 */
public interface ServerHttpResponse extends HttpOutputMessage, Flushable, Closeable {

  /**
   * Set the HTTP status code of the response.
   *
   * @param status the HTTP status as an HttpStatus enum value
   */
  void setStatusCode(HttpStatusCode status);

  /**
   * Ensure that the headers and the content of the response are written out.
   * <p>After the first flush, headers can no longer be changed.
   * Only further content writing and content flushing is possible.
   * <p>
   * NOTE: Not recommended to use {@link OutputStream#flush() getBody().flush()}
   */
  @Override
  void flush() throws IOException;

  /**
   * Close this response, freeing any resources created.
   */
  @Override
  void close();

}
