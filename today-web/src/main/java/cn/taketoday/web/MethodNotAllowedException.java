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

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * Exception for errors that fit response status 405 (method not allowed).
 *
 * @author TODAY
 * @since 2018-7-1 19:38:39
 */
public class MethodNotAllowedException extends ResponseStatusException {
  @Serial
  private static final long serialVersionUID = 1L;

  private final String method;

  private final Set<HttpMethod> supportedMethods;

  public MethodNotAllowedException(HttpMethod method, Collection<HttpMethod> supportedMethods) {
    this(method.name(), supportedMethods);
  }

  public MethodNotAllowedException(String method, @Nullable Collection<HttpMethod> supportedMethods) {
    super(HttpStatus.METHOD_NOT_ALLOWED, "Request method '" + method + "' is not supported.");
    Assert.notNull(method, "'method' is required");
    if (supportedMethods == null) {
      supportedMethods = Collections.emptySet();
    }
    this.method = method;
    this.supportedMethods = Collections.unmodifiableSet(new LinkedHashSet<>(supportedMethods));

    setDetail(this.supportedMethods.isEmpty() ? getReason() :
              "Supported methods: " + this.supportedMethods.stream()
                      .map(HttpMethod::toString)
                      .collect(Collectors.joining("', '", "'", "'")));
  }

  /**
   * Return HttpHeaders with an "Allow" header that documents the allowed
   * HTTP methods for this URL, if available, or an empty instance otherwise.
   */
  @Override
  public HttpHeaders getHeaders() {
    if (CollectionUtils.isEmpty(this.supportedMethods)) {
      return HttpHeaders.empty();
    }
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setAllow(this.supportedMethods);
    return headers;
  }

  /**
   * Return the HTTP method for the failed request.
   */
  public String getHttpMethod() {
    return this.method;
  }

  /**
   * Return the list of supported HTTP methods.
   */
  public Set<HttpMethod> getSupportedMethods() {
    return this.supportedMethods;
  }

}
