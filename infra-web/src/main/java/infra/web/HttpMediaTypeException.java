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

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

import infra.core.NestedRuntimeException;
import infra.http.MediaType;
import infra.http.ProblemDetail;
import infra.lang.Unmodifiable;

/**
 * Abstract base for exceptions related to media types. Adds a list of supported {@link MediaType MediaTypes}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 20:03
 */
public abstract class HttpMediaTypeException extends NestedRuntimeException implements ErrorResponse {

  private final Collection<MediaType> supportedMediaTypes;

  private final ProblemDetail body = ProblemDetail.forStatus(getStatusCode());

  private final String messageDetailCode;

  private final Object @Nullable [] messageDetailArguments;

  /**
   * Create a new HttpMediaTypeException with a list of supported media types.
   *
   * @param supportedMediaTypes the list of supported media types
   * @param messageDetailCode the code to use to resolve the problem "detail"
   * through a {@link infra.context.MessageSource}
   * @param messageDetailArguments the arguments to make available when
   * resolving the problem "detail" through a {@code MessageSource}
   * @since 5.0
   */
  protected HttpMediaTypeException(@Nullable String message, Collection<MediaType> supportedMediaTypes,
          @Nullable String messageDetailCode, Object @Nullable [] messageDetailArguments) {

    super(message);
    this.messageDetailArguments = messageDetailArguments;
    this.supportedMediaTypes = Collections.unmodifiableCollection(supportedMediaTypes);
    this.messageDetailCode = messageDetailCode != null ? messageDetailCode : ErrorResponse.getDefaultDetailMessageCode(getClass(), null);
  }

  /**
   * Return the list of supported media types.
   */
  @Unmodifiable
  public Collection<MediaType> getSupportedMediaTypes() {
    return this.supportedMediaTypes;
  }

  @Override
  public ProblemDetail getBody() {
    return this.body;
  }

  @Override
  public String getDetailMessageCode() {
    return this.messageDetailCode;
  }

  @Override
  public Object @Nullable [] getDetailMessageArguments() {
    return this.messageDetailArguments;
  }

}
