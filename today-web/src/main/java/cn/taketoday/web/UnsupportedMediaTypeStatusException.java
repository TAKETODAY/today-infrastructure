/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * Exception for errors that fit response status 415 (unsupported media type).
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 15:59
 */
@SuppressWarnings("serial")
public class UnsupportedMediaTypeStatusException extends ResponseStatusException {

  @Nullable
  private final MediaType contentType;

  private final List<MediaType> supportedMediaTypes;

  @Nullable
  private final ResolvableType bodyType;

  @Nullable
  private final HttpMethod method;

  /**
   * Constructor for when the specified Content-Type is invalid.
   */
  public UnsupportedMediaTypeStatusException(@Nullable String reason) {
    this(reason, Collections.emptyList());
  }

  /**
   * Constructor for when the specified Content-Type is invalid.
   */
  public UnsupportedMediaTypeStatusException(@Nullable String reason, List<MediaType> supportedTypes) {
    super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, reason);
    this.contentType = null;
    this.supportedMediaTypes = Collections.unmodifiableList(supportedTypes);
    this.bodyType = null;
    this.method = null;
    setDetail("Could not parse Content-Type.");
  }

  /**
   * Constructor for when the Content-Type can be parsed but is not supported.
   */
  public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes) {
    this(contentType, supportedTypes, null, null);
  }

  /**
   * Constructor for when trying to encode from or decode to a specific Java type.
   */
  public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
          @Nullable ResolvableType bodyType) {
    this(contentType, supportedTypes, bodyType, null);
  }

  /**
   * Constructor that provides the HTTP method.
   */
  public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
          @Nullable HttpMethod method) {
    this(contentType, supportedTypes, null, method);
  }

  /**
   * Constructor for when trying to encode from or decode to a specific Java type.
   */
  public UnsupportedMediaTypeStatusException(@Nullable MediaType contentType, List<MediaType> supportedTypes,
          @Nullable ResolvableType bodyType, @Nullable HttpMethod method) {

    super(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Content type '" + (contentType != null ? contentType : "") + "' not supported" +
                    (bodyType != null ? " for bodyType=" + bodyType : ""));

    this.contentType = contentType;
    this.supportedMediaTypes = Collections.unmodifiableList(supportedTypes);
    this.bodyType = bodyType;
    this.method = method;

    setDetail(contentType != null ? "Content-Type '" + contentType + "' is not supported." : null);
  }

  /**
   * Return the request Content-Type header if it was parsed successfully,
   * or {@code null} otherwise.
   */
  @Nullable
  public MediaType getContentType() {
    return this.contentType;
  }

  /**
   * Return the list of supported content types in cases when the Content-Type
   * header is parsed but not supported, or an empty list otherwise.
   */
  public List<MediaType> getSupportedMediaTypes() {
    return this.supportedMediaTypes;
  }

  /**
   * Return the body type in the context of which this exception was generated.
   * <p>This is applicable when the exception was raised as a result trying to
   * encode from or decode to a specific Java type.
   *
   * @return the body type, or {@code null} if not available
   */
  @Nullable
  public ResolvableType getBodyType() {
    return this.bodyType;
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
    if (this.method == HttpMethod.PATCH) {
      headers.setAcceptPatch(this.supportedMediaTypes);
    }
    return headers;
  }

}
