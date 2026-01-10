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
