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

package cn.taketoday.web;

import java.util.Collections;
import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.CollectionUtils;

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
  public HttpMediaTypeNotAcceptableException(List<MediaType> mediaTypes) {
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
