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

package infra.web.service.invoker;

import org.jspecify.annotations.Nullable;

import infra.core.MethodParameter;
import infra.core.ParameterizedTypeReference;
import infra.core.ReactiveAdapter;
import infra.core.ReactiveAdapterRegistry;
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

  private final @Nullable ReactiveAdapterRegistry registry;

  /**
   * Constructor with a {@link ReactiveAdapterRegistry}
   */
  public RequestBodyArgumentResolver(@Nullable ReactiveAdapterRegistry registry) {
    this.registry = registry;
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
      if (registry != null) {
        ReactiveAdapter adapter = registry.getAdapter(parameter.getParameterType());
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
