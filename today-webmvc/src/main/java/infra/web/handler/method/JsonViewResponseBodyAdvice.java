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

import com.fasterxml.jackson.annotation.JsonView;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

import infra.core.MethodParameter;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.SmartHttpMessageConverter;
import infra.lang.Assert;

/**
 * A {@link ResponseBodyAdvice} implementation that adds support for Jackson's
 * {@code @JsonView} annotation declared on MVC {@code @RequestMapping} or
 * {@code @ExceptionHandler} method.
 *
 * <p>Note that despite {@code @JsonView} allowing for more than one class to
 * be specified, the use for a response body advice is only supported with
 * exactly one class argument. Consider the use of a composite interface.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see com.fasterxml.jackson.annotation.JsonView
 * @see tools.jackson.databind.ObjectMapper#writerWithView(Class)
 * @since 4.0 2022/2/11 21:02
 */
public class JsonViewResponseBodyAdvice extends AbstractJacksonResponseBodyAdvice {

  @Override
  public boolean supports(@Nullable Object body, @Nullable MethodParameter returnType, HttpMessageConverter<?> converter) {
    return super.supports(body, returnType, converter)
            && returnType != null
            && returnType.hasMethodAnnotation(JsonView.class);
  }

  @Override
  public @Nullable Map<String, Object> determineWriteHints(@Nullable Object body, @Nullable MethodParameter returnType,
          MediaType selectedContentType, SmartHttpMessageConverter<?> selected) {
    Class<?> jsonView = getJsonView(returnType);
    if (jsonView != null) {
      return Collections.singletonMap(JsonView.class.getName(), jsonView);
    }
    return null;
  }

  private static @Nullable Class<?> getJsonView(@Nullable MethodParameter returnType) {
    if (returnType == null) {
      return null;
    }
    JsonView ann = returnType.getMethodAnnotation(JsonView.class);
    Assert.state(ann != null, "No JsonView annotation");

    Class<?>[] classes = ann.value();
    if (classes.length != 1) {
      throw new IllegalArgumentException(
              "@JsonView only supported for response body advice with exactly 1 class argument: " + returnType);
    }
    return classes[0];
  }

}
