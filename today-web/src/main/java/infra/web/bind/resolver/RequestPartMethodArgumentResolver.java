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
import infra.web.multipart.MultipartException;
import infra.web.multipart.MultipartFile;
import infra.web.multipart.Part;

/**
 * Resolves the following method arguments:
 * <ul>
 * <li>Annotated with @{@link RequestPart}
 * <li>Of type {@link MultipartFile}
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
   * <li>of type {@link MultipartFile} unless annotated with {@code @RequestParam}
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
        throw new MultipartException("Current request is not a multipart request");
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
