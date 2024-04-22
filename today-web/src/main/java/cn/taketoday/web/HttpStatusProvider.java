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

import cn.taketoday.core.Pair;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.annotation.ResponseStatus;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0.1 2021/5/6 19:17
 */
public interface HttpStatusProvider {

  /**
   * Return the HTTP status code to use for the response.
   */
  HttpStatusCode getStatusCode();

  static Pair<HttpStatusCode, String> getStatusCode(Throwable ex) {
    if (ex instanceof HttpStatusProvider provider) {
      return Pair.of(provider.getStatusCode(), ex.getMessage());
    }
    if (ex instanceof ConversionException) {
      return Pair.of(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    ResponseStatus status = AnnotationUtils.getAnnotation(ex.getClass(), ResponseStatus.class);
    if (status != null) {
      String reason = status.reason();
      return Pair.of(status.code(), StringUtils.hasText(reason) ? reason : ex.getMessage());
    }
    return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
  }
}
