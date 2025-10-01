/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.service.invoker;

import org.jspecify.annotations.Nullable;

import infra.core.MethodParameter;
import infra.core.ParameterizedTypeReference;
import infra.core.ReactiveAdapter;
import infra.core.ReactiveAdapterRegistry;
import infra.core.ReactiveStreams;
import infra.http.StreamingHttpOutputMessage;
import infra.lang.Assert;
import infra.web.annotation.RequestBody;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestBody @RequestBody}
 * annotated arguments.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RequestBodyArgumentResolver implements HttpServiceArgumentResolver {

  @Nullable
  private final ReactiveAdapterRegistry reactiveAdapterRegistry;

  /**
   * Constructor with a {@link HttpExchangeAdapter}, for access to config settings.
   */
  public RequestBodyArgumentResolver(HttpExchangeAdapter exchangeAdapter) {
    if (ReactiveStreams.reactorPresent) {
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
  public boolean resolve(@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
    if (parameter.getParameterType().equals(StreamingHttpOutputMessage.Body.class)) {
      requestValues.setBodyValue(argument);
      return true;
    }

    RequestBody annot = parameter.getParameterAnnotation(RequestBody.class);
    if (annot == null) {
      return false;
    }

    if (argument != null) {
      if (this.reactiveAdapterRegistry != null) {
        ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(parameter.getParameterType());
        if (adapter != null) {
          MethodParameter nestedParam = parameter.nested();

          String message = "Async type for @RequestBody should produce value(s)";
          Assert.isTrue(!adapter.isNoValue(), message);
          Assert.isTrue(nestedParam.getNestedParameterType() != Void.class, message);

          if (requestValues instanceof ReactiveHttpRequestValues.Builder rrv) {
            rrv.setBodyPublisher(
                    adapter.toPublisher(argument), asParameterizedTypeRef(nestedParam));
          }
          else {
            throw new IllegalStateException(
                    "RequestBody with a reactive type is only supported with reactive client");
          }

          return true;
        }
      }

      // Not a reactive type
      requestValues.setBodyValue(argument, asParameterizedTypeRef(parameter));
    }

    return true;
  }

  private static ParameterizedTypeReference<Object> asParameterizedTypeRef(MethodParameter nestedParam) {
    return ParameterizedTypeReference.forType(nestedParam.getNestedGenericParameterType());
  }

}
