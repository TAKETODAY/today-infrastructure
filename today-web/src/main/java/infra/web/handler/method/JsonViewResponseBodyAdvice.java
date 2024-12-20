/*
 * Copyright 2017 - 2023 the original author or authors.
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

import infra.core.MethodParameter;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.json.MappingJacksonValue;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.web.RequestContext;

/**
 * A {@link ResponseBodyAdvice} implementation that adds support for Jackson's
 * {@code @JsonView} annotation declared on MVC {@code @RequestMapping} or
 * {@code @ExceptionHandler} method.
 *
 * <p>The serialization view specified in the annotation will be passed in to the
 * {@link infra.http.converter.json.MappingJackson2HttpMessageConverter}
 * which will then use it to serialize the response body.
 *
 * <p>Note that despite {@code @JsonView} allowing for more than one class to
 * be specified, the use for a response body advice is only supported with
 * exactly one class argument. Consider the use of a composite interface.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see com.fasterxml.jackson.annotation.JsonView
 * @see com.fasterxml.jackson.databind.ObjectMapper#writerWithView(Class)
 * @since 4.0 2022/2/11 21:02
 */
public class JsonViewResponseBodyAdvice extends AbstractMappingJacksonResponseBodyAdvice {

  @Override
  public boolean supports(@Nullable Object body, @Nullable MethodParameter returnType, HttpMessageConverter<?> converter) {
    return super.supports(body, returnType, converter)
            && returnType != null
            && returnType.hasMethodAnnotation(JsonView.class);
  }

  @Override
  protected void beforeBodyWriteInternal(MappingJacksonValue value,
          MediaType contentType, MethodParameter returnType, RequestContext request) {

    JsonView ann = returnType.getMethodAnnotation(JsonView.class);
    Assert.state(ann != null, "No JsonView annotation");

    Class<?>[] classes = ann.value();
    if (classes.length != 1) {
      throw new IllegalArgumentException(
              "@JsonView only supported for response body advice with exactly 1 class argument: " + returnType);
    }

    value.setSerializationView(classes[0]);
  }

}
