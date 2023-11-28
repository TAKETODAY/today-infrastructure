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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.method;

import com.fasterxml.jackson.annotation.JsonView;

import java.io.IOException;
import java.lang.reflect.Type;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.json.AbstractJackson2HttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJacksonInputMessage;
import cn.taketoday.lang.Assert;

/**
 * A {@link RequestBodyAdvice} implementation that adds support for Jackson's
 * {@code @JsonView} annotation declared on MVC {@code @HttpEntity} or
 * {@code @RequestBody} method parameter.
 *
 * <p>The deserialization view specified in the annotation will be passed in to the
 * {@link cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter}
 * which will then use it to deserialize the request body with.
 *
 * <p>Note that despite {@code @JsonView} allowing for more than one class to
 * be specified, the use for a request body advice is only supported with
 * exactly one class argument. Consider the use of a composite interface.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see com.fasterxml.jackson.annotation.JsonView
 * @see com.fasterxml.jackson.databind.ObjectMapper#readerWithView(Class)
 * @since 4.0 2022/2/11 21:08
 */
public class JsonViewRequestBodyAdvice implements RequestBodyAdvice {

  @Override
  public boolean supports(MethodParameter methodParameter, Type targetType, HttpMessageConverter<?> converter) {

    return converter instanceof AbstractJackson2HttpMessageConverter
            && methodParameter.getParameterAnnotation(JsonView.class) != null;
  }

  @Override
  public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter methodParameter,
          Type targetType, HttpMessageConverter<?> selectedConverterType) throws IOException {

    JsonView ann = methodParameter.getParameterAnnotation(JsonView.class);
    Assert.state(ann != null, "No JsonView annotation");

    Class<?>[] classes = ann.value();
    if (classes.length != 1) {
      throw new IllegalArgumentException(
              "@JsonView only supported for request body advice with exactly 1 class argument: " + methodParameter);
    }

    return new MappingJacksonInputMessage(inputMessage.getBody(), inputMessage.getHeaders(), classes[0]);
  }

}
