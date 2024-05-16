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

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.lang.Nullable;

/**
 * For Framework Configuration errors
 *
 * @author TODAY 2021/4/26 22:20
 * @since 3.0
 */
public class InfraConfigurationException extends NestedRuntimeException implements ErrorResponse {

  private final ProblemDetail body = ProblemDetail.forStatusAndDetail(getStatusCode(), getMessage());

  public InfraConfigurationException(@Nullable String message) {
    super(message);
  }

  public InfraConfigurationException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  @Override
  public ProblemDetail getBody() {
    return body;
  }

}
