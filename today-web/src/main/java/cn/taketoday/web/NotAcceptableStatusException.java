/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.CollectionUtils;

/**
 * Exception for errors that fit response status 406 (not acceptable).
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 16:20
 */
@SuppressWarnings("serial")
public class NotAcceptableStatusException extends ResponseStatusException {

  private final List<MediaType> supportedMediaTypes;

  /**
   * Constructor for when the requested Content-Type is invalid.
   */
  public NotAcceptableStatusException(String reason) {
    super(HttpStatus.NOT_ACCEPTABLE, reason);
    this.supportedMediaTypes = Collections.emptyList();
    getBody().setDetail("Could not parse Accept header.");
  }

  /**
   * Constructor for when the requested Content-Type is not supported.
   */
  public NotAcceptableStatusException(List<MediaType> mediaTypes) {
    super(HttpStatus.NOT_ACCEPTABLE, "Could not find acceptable representation");
    this.supportedMediaTypes = Collections.unmodifiableList(mediaTypes);
    getBody().setDetail("Acceptable representations: " +
            mediaTypes.stream().map(MediaType::toString).collect(Collectors.joining(", ", "'", "'")) + ".");
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
    HttpHeaders headers = HttpHeaders.create();
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
