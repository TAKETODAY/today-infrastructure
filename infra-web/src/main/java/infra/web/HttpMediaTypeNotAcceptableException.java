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

package infra.web;

import java.util.Collection;
import java.util.Collections;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.util.CollectionUtils;

/**
 * Exception thrown when the request handler cannot generate a response that is acceptable by the client.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 20:15
 */
public class HttpMediaTypeNotAcceptableException extends HttpMediaTypeException {

  private static final String PARSE_ERROR_DETAIL_CODE =
          ErrorResponse.getDefaultDetailMessageCode(HttpMediaTypeNotAcceptableException.class, "parseError");

  /**
   * Constructor for when the {@code Accept} header cannot be parsed.
   *
   * @param message the parse error message
   */
  public HttpMediaTypeNotAcceptableException(String message) {
    super(message, Collections.emptyList(), PARSE_ERROR_DETAIL_CODE, null);
    getBody().setDetail("Could not parse Accept header.");
  }

  /**
   * Create a new HttpMediaTypeNotSupportedException.
   *
   * @param mediaTypes the list of supported media types
   */
  public HttpMediaTypeNotAcceptableException(Collection<MediaType> mediaTypes) {
    super("No acceptable representation", mediaTypes, null, new Object[] { mediaTypes });
    getBody().setDetail("Acceptable representations: %s.".formatted(mediaTypes));
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatus.NOT_ACCEPTABLE;
  }

  @Override
  public HttpHeaders getHeaders() {
    if (CollectionUtils.isEmpty(getSupportedMediaTypes())) {
      return HttpHeaders.empty();
    }
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setAccept(getSupportedMediaTypes());
    return headers;
  }

}
