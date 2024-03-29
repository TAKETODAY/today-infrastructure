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

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ProblemDetail;

/**
 * For Framework Configuration errors
 *
 * @author TODAY 2021/4/26 22:20
 * @since 3.0
 */
public class InfraConfigurationException extends NestedRuntimeException implements ErrorResponse {
  @Serial
  private static final long serialVersionUID = 1L;

  public InfraConfigurationException() {
    super();
  }

  public InfraConfigurationException(String message) {
    super(message);
  }

  public InfraConfigurationException(Throwable cause) {
    super(cause);
  }

  public InfraConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  @Override
  public ProblemDetail getBody() {
    return ProblemDetail.forStatus(getStatusCode());
  }

}
