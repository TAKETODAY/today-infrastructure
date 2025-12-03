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
import org.reactivestreams.Publisher;

import infra.core.MethodParameter;
import infra.core.ParameterizedTypeReference;
import infra.core.ReactiveAdapter;
import infra.core.ReactiveAdapterRegistry;
import infra.core.ReactiveStreams;
import infra.core.io.Resource;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.lang.Assert;
import infra.web.annotation.RequestPart;
import infra.web.multipart.MultipartFile;
import infra.web.multipart.Part;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestPart @RequestPart}
 * annotated arguments.
 *
 * <p>The argument may be:
 * <ul>
 * <li>String -- form field
 * <li>{@link Resource Resource} -- file part
 * <li>{@link MultipartFile} -- uploaded file
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

  @Nullable
  private final ReactiveAdapterRegistry reactiveAdapterRegistry;

  /**
   * Constructor with a {@link HttpExchangeAdapter}, for access to config settings.
   */
  public RequestPartArgumentResolver(HttpExchangeAdapter exchangeAdapter) {
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

  @Nullable
  @Override
  protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
    RequestPart annot = parameter.getParameterAnnotation(RequestPart.class);
    boolean isMultiPartFile = parameter.getParameterType().equals(MultipartFile.class);
    String label = isMultiPartFile ? "MultipartFile" : "request part";

    if (annot != null) {
      return new NamedValueInfo(annot.name(), annot.required(), null, label, true);
    }
    else if (isMultiPartFile) {
      return new NamedValueInfo("", true, null, label, true);
    }

    return null;
  }

  @Override
  protected void addRequestValue(String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
    if (reactiveAdapterRegistry != null) {
      ReactiveAdapter adapter = reactiveAdapterRegistry.getAdapter(parameter.getParameterType());
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
