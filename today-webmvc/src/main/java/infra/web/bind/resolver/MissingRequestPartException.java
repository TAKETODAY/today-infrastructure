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

package infra.web.bind.resolver;

import infra.http.HttpStatus;
import infra.web.ErrorResponse;
import infra.web.bind.MissingRequestValueException;

/**
 * Raised when the part of a "multipart/form-data" request identified by its
 * name cannot be found.
 *
 * <p>This may be because the request is not a multipart/form-data request,
 * because the part is not present in the request, or because the web
 * application is not configured correctly for processing multipart requests,
 * e.g. no {@code MultipartResolver}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MissingRequestPartException extends MissingRequestValueException implements ErrorResponse {

  private final String requestPartName;

  /**
   * Constructor for MissingRequestPartException.
   *
   * @param requestPartName the name of the missing part of the multipart request
   */
  public MissingRequestPartException(String requestPartName) {
    super("Required part '%s' is not present.".formatted(requestPartName), false, null, new Object[] { requestPartName });
    this.requestPartName = requestPartName;
    getBody().setDetail(getMessage());
  }

  /**
   * Return the name of the offending part of the multipart request.
   */
  public String getRequestPartName() {
    return this.requestPartName;
  }

  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.BAD_REQUEST;
  }

}
