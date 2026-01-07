/*
 * Copyright 2017 - 2026 the original author or authors.
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
