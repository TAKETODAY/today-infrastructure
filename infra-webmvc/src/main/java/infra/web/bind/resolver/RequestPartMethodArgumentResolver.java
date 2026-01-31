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

package infra.web.bind.resolver;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import infra.core.MethodParameter;
import infra.http.converter.HttpMessageConverter;
import infra.validation.annotation.ValidationAnnotationUtils;
import infra.web.RequestContext;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RequestPart;
import infra.web.bind.MethodArgumentNotValidException;
import infra.web.handler.method.NamedValueInfo;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.server.MultipartException;
import infra.web.server.NotMultipartRequestException;
import infra.web.multipart.Part;

/**
 * Resolves the following method arguments:
 * <ul>
 * <li>Annotated with @{@link RequestPart}
 * <li>Of type {@link Part}
 * </ul>
 *
 * <p>When a parameter is annotated with {@code @RequestPart}, the content of the part is
 * passed through an {@link HttpMessageConverter} to resolve the method argument with the
 * 'Content-Type' of the request part in mind. This is analogous to what @{@link RequestBody}
 * does to resolve an argument based on the content of a regular request.
 *
 * <p>When a parameter is not annotated with {@code @RequestPart} or the name of
 * the part is not specified, the request part's name is derived from the name of
 * the method argument.
 *
 * <p>Automatic validation may be applied if the argument is annotated with any
 * {@linkplain ValidationAnnotationUtils#determineValidationHints
 * annotations that trigger validation}. In case of validation failure, a
 * {@link MethodArgumentNotValidException} is raised and a 400 response status code returned if the
 * {@link infra.web.handler.SimpleHandlerExceptionHandler}
 * is configured.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 15:32
 */
public class RequestPartMethodArgumentResolver extends AbstractMessageConverterMethodArgumentResolver {

  /**
   * Basic constructor with converters only.
   */
  public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters) {
    super(messageConverters);
  }

  /**
   * Constructor with converters and {@code RequestBodyAdvice} and
   * {@code ResponseBodyAdvice}.
   */
  public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters,
          List<Object> requestResponseBodyAdvice) {

    super(messageConverters, requestResponseBodyAdvice);
  }

  /**
   * Whether the given {@linkplain MethodParameter method parameter} is
   * supported as multi-part. Supports the following method parameters:
   * <ul>
   * <li>annotated with {@code @RequestPart}
   * <li>of type {@link Part} unless annotated with {@code @RequestParam}
   * {@code @RequestParam}
   * </ul>
   */
  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    if (parameter.hasParameterAnnotation(RequestPart.class)) {
      return true;
    }
    else {
      if (parameter.hasParameterAnnotation(RequestParam.class)) {
        return false;
      }
      return MultipartResolutionDelegate.isMultipartArgument(parameter.getParameter());
    }
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    MethodParameter parameter = resolvable.getParameter();

    NamedValueInfo namedValueInfo = resolvable.getNamedValueInfo();
    String name = namedValueInfo.name;
    Object arg = null;

    Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, context);
    if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
      arg = mpArg;
    }
    else {
      try {
        Part part = context.asMultipartRequest().getPart(name);
        if (part == null) {
          throw new MissingRequestPartException(name);
        }
        arg = readWithMessageConverters(part, parameter, parameter.getNestedGenericParameterType());
        validateIfApplicable(context, parameter, arg);
      }
      catch (MissingRequestPartException | MultipartException ex) {
        if (namedValueInfo.required) {
          throw ex;
        }
      }
    }

    if (arg == null && namedValueInfo.required) {
      if (!context.isMultipart()) {
        throw new NotMultipartRequestException("Current request is not a multipart request", null);
      }
      else {
        throw new MissingRequestPartException(name);
      }
    }
    return arg;
  }

  @Override
  void closeStreamIfNecessary(InputStream body) {
    // RequestPartServerHttpRequest exposes individual part streams,
    // potentially from temporary files -> explicit close call after resolution
    // in order to prevent file descriptor leaks.
    try {
      body.close();
    }
    catch (IOException ex) {
      // ignore
    }
  }

}
