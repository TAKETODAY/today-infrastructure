/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.service.invoker;

import org.reactivestreams.Publisher;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.annotation.RequestBody;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestBody @RequestBody}
 * annotated arguments.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class RequestBodyArgumentResolver implements HttpServiceArgumentResolver {

  private final ReactiveAdapterRegistry reactiveAdapterRegistry;

  public RequestBodyArgumentResolver(ReactiveAdapterRegistry reactiveAdapterRegistry) {
    Assert.notNull(reactiveAdapterRegistry, "ReactiveAdapterRegistry is required");
    this.reactiveAdapterRegistry = reactiveAdapterRegistry;
  }

  @Override
  public boolean resolve(
          @Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

    RequestBody annot = parameter.getParameterAnnotation(RequestBody.class);
    if (annot == null) {
      return false;
    }

    if (argument != null) {
      ReactiveAdapter reactiveAdapter = this.reactiveAdapterRegistry.getAdapter(parameter.getParameterType());
      if (reactiveAdapter != null) {
        setBody(argument, parameter, reactiveAdapter, requestValues);
      }
      else {
        requestValues.setBodyValue(argument);
      }
    }

    return true;
  }

  private <E> void setBody(
          Object argument, MethodParameter parameter, ReactiveAdapter reactiveAdapter,
          HttpRequestValues.Builder requestValues) {

    String message = "Async type for @RequestBody should produce value(s)";
    Assert.isTrue(!reactiveAdapter.isNoValue(), message);

    parameter = parameter.nested();
    Class<?> elementClass = parameter.getNestedParameterType();
    Assert.isTrue(elementClass != Void.class, message);
    ParameterizedTypeReference<E> typeRef = ParameterizedTypeReference.forType(parameter.getNestedGenericParameterType());
    Publisher<E> publisher = reactiveAdapter.toPublisher(argument);

    requestValues.setBody(publisher, typeRef);
  }

}
