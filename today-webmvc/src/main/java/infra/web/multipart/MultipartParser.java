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

package infra.web.multipart;

import infra.web.RequestContext;

/**
 * Strategy interface for parsing multipart requests.
 *
 * <p>Implementations of this interface are responsible for parsing
 * multipart requests and creating {@link MultipartRequest} objects
 * that provide access to the parsed parts.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/2 15:02
 */
public interface MultipartParser {

  /**
   * Parse the given multipart request context into a MultipartRequest.
   *
   * @param request the request context to parse
   * @return the parsed MultipartRequest
   * @throws MultipartException if parsing fails
   */
  MultipartRequest parse(RequestContext request) throws MultipartException;

}
