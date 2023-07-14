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

package cn.taketoday.web.service.invoker;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.annotation.RequestBody;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestBody @RequestBody}
 * annotated arguments.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RequestBodyArgumentResolver implements HttpServiceArgumentResolver {

  private static final boolean REACTOR_PRESENT =
          ClassUtils.isPresent("reactor.core.publisher.Mono", RequestBodyArgumentResolver.class.getClassLoader());

  @Nullable
  private final ReactiveAdapterRegistry reactiveAdapterRegistry;

  /**
   * Constructor with a {@link HttpExchangeAdapter}, for access to config settings.
   */
  public RequestBodyArgumentResolver(HttpExchangeAdapter exchangeAdapter) {
    if (REACTOR_PRESENT) {
      this.reactiveAdapterRegistry =
              (exchangeAdapter instanceof ReactorHttpExchangeAdapter reactorAdapter ?
               reactorAdapter.getReactiveAdapterRegistry() :
               ReactiveAdapterRegistry.getSharedInstance());
    }
    else {
      this.reactiveAdapterRegistry = null;
    }
  }

  @Override
  public boolean resolve(
          @Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

    RequestBody annot = parameter.getParameterAnnotation(RequestBody.class);
    if (annot == null) {
      return false;
    }

    if (argument != null) {
      if (this.reactiveAdapterRegistry != null) {
        ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(parameter.getParameterType());
        if (adapter != null) {
          MethodParameter nestedParameter = parameter.nested();

          String message = "Async type for @RequestBody should produce value(s)";
          Assert.isTrue(!adapter.isNoValue(), message);
          Assert.isTrue(nestedParameter.getNestedParameterType() != Void.class, message);

          if (requestValues instanceof ReactiveHttpRequestValues.Builder reactiveRequestValues) {
            reactiveRequestValues.setBodyPublisher(
                    adapter.toPublisher(argument), asParameterizedTypeRef(nestedParameter));
          }
          else {
            throw new IllegalStateException(
                    "RequestBody with a reactive type is only supported with reactive client");
          }

          return true;
        }
      }

      // Not a reactive type
      requestValues.setBodyValue(argument);
    }

    return true;
  }

  private static ParameterizedTypeReference<Object> asParameterizedTypeRef(MethodParameter nestedParam) {
    return ParameterizedTypeReference.forType(nestedParam.getNestedGenericParameterType());
  }

}
