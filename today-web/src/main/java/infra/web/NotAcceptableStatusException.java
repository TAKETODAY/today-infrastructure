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

package infra.web;

import java.util.Collections;
import java.util.List;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.util.CollectionUtils;

/**
 * Exception for errors that fit response status 406 (not acceptable).
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 16:20
 */
public class NotAcceptableStatusException extends ResponseStatusException {

  private static final String PARSE_ERROR_DETAIL_CODE =
          ErrorResponse.getDefaultDetailMessageCode(NotAcceptableStatusException.class, "parseError");

  private final List<MediaType> supportedMediaTypes;

  /**
   * Constructor for when the requested Content-Type is invalid.
   */
  public NotAcceptableStatusException(String reason) {
    super(HttpStatus.NOT_ACCEPTABLE, reason, null, PARSE_ERROR_DETAIL_CODE, null);
    this.supportedMediaTypes = Collections.emptyList();
    setDetail("Could not parse Accept header.");
  }

  /**
   * Constructor for when the requested Content-Type is not supported.
   */
  public NotAcceptableStatusException(List<MediaType> mediaTypes) {
    super(HttpStatus.NOT_ACCEPTABLE, "Could not find acceptable representation", null, null, new Object[] { mediaTypes });
    this.supportedMediaTypes = Collections.unmodifiableList(mediaTypes);
    setDetail("Acceptable representations: %s.".formatted(mediaTypes));
  }

  /**
   * Return HttpHeaders with an "Accept" header that documents the supported
   * media types, if available, or an empty instance otherwise.
   */
  @Override
  public HttpHeaders getHeaders() {
    if (CollectionUtils.isEmpty(this.supportedMediaTypes)) {
      return HttpHeaders.empty();
    }
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setAccept(this.supportedMediaTypes);
    return headers;
  }

  /**
   * Return the list of supported content types in cases when the Accept
   * header is parsed but not supported, or an empty list otherwise.
   */
  public List<MediaType> getSupportedMediaTypes() {
    return this.supportedMediaTypes;
  }

}
