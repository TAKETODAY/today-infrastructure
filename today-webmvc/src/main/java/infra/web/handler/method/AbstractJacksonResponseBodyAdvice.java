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

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import infra.core.MethodParameter;
import infra.http.MediaType;
import infra.http.converter.AbstractJacksonHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.web.RequestContext;

/**
 * A convenient base class for {@code ResponseBodyAdvice} implementations
 * that customize the response before JSON serialization with
 * {@link AbstractJacksonHttpMessageConverter}'s concrete subclasses.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/11 20:59
 */
public abstract class AbstractJacksonResponseBodyAdvice implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(@Nullable Object body, @Nullable MethodParameter returnType, HttpMessageConverter<?> converter) {
    return converter instanceof AbstractJacksonHttpMessageConverter;
  }

  @Nullable
  @Override
  public Object beforeBodyWrite(@Nullable Object body, @Nullable MethodParameter returnType,
          MediaType contentType, HttpMessageConverter<?> selected, RequestContext context) {

    return body;
  }

}
