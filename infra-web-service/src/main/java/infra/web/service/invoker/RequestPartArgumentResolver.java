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
import org.reactivestreams.Publisher;

import infra.core.MethodParameter;
import infra.core.ParameterizedTypeReference;
import infra.core.ReactiveAdapter;
import infra.core.ReactiveAdapterRegistry;
import infra.core.io.Resource;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.lang.Assert;
import infra.web.annotation.RequestPart;
import infra.web.multipart.Part;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestPart @RequestPart}
 * annotated arguments.
 *
 * <p>The argument may be:
 * <ul>
 * <li>String -- form field
 * <li>{@link Resource Resource} -- file part
 * <li>{@link Part} -- uploaded file
 * <li>Object -- content to be encoded (e.g. to JSON)
 * <li>{@link HttpEntity} -- part content and headers although generally it's
 * easier to add headers through the returned builder
 * <li>{@link Part} -- a part from a server request
 * <li>{@link Publisher} of any of the above
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/3 14:20
 */
public class RequestPartArgumentResolver extends AbstractNamedValueArgumentResolver {

  private final @Nullable ReactiveAdapterRegistry registry;

  /**
   * Constructor with a {@link HttpExchangeAdapter}, for access to config settings.
   */
  public RequestPartArgumentResolver(@Nullable ReactiveAdapterRegistry registry) {
    this.registry = registry;
  }

  @Override
  protected @Nullable NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
    RequestPart annot = parameter.getParameterAnnotation(RequestPart.class);
    boolean isMultipart = parameter.getParameterType().equals(Part.class);
    String label = "request part";

    if (annot != null) {
      return new NamedValueInfo(annot.name(), annot.required(), null, label, true);
    }
    else if (isMultipart) {
      return new NamedValueInfo("", true, null, label, true);
    }

    return null;
  }

  @Override
  protected void addRequestValue(String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
    if (registry != null) {
      ReactiveAdapter adapter = registry.getAdapter(parameter.getParameterType());
      if (adapter != null) {
        String message = "Async type for @RequestPart should produce value(s)";
        Assert.isTrue(!adapter.isNoValue(), message);

        MethodParameter nestedParameter = parameter.nested();
        Assert.isTrue(nestedParameter.getNestedParameterType() != Void.class, message);

        if (requestValues instanceof ReactiveHttpRequestValues.Builder reactiveValues) {
          reactiveValues.addRequestPartPublisher(
                  name, adapter.toPublisher(value), asParameterizedTypeRef(nestedParameter));
        }
        else {
          throw new IllegalStateException(
                  "RequestPart with a reactive type is only supported with reactive client");
        }
        return;
      }
    }

    if (value instanceof Part part) {
      value = toHttpEntity(name, part);
    }

    requestValues.addRequestPart(name, value);
  }

  private static ParameterizedTypeReference<Object> asParameterizedTypeRef(MethodParameter nestedParam) {
    return ParameterizedTypeReference.forType(nestedParam.getNestedGenericParameterType());
  }

  private static Object toHttpEntity(String name, Part part) {
    HttpHeaders headers = HttpHeaders.forWritable();
    if (part.getOriginalFilename() != null) {
      headers.setContentDispositionFormData(name, part.getOriginalFilename());
    }
    if (part.getContentType() != null) {
      headers.setContentType(part.getContentType());
    }
    return new HttpEntity<>(part.getResource(), headers);
  }

}
