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

package infra.web;

import org.jspecify.annotations.Nullable;

import infra.core.NestedRuntimeException;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;

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
