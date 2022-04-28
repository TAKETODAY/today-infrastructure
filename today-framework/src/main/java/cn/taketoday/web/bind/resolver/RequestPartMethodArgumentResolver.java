/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.bind.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.RequestPart;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.MultipartException;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * Resolves the following method arguments:
 * <ul>
 * <li>Annotated with @{@link RequestPart}
 * <li>Of type {@link MultipartFile}
 * <li>Of type {@code jakarta.servlet.http.Part} in conjunction with Servlet multipart requests
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
 * {@linkplain cn.taketoday.validation.annotation.ValidationAnnotationUtils#determineValidationHints
 * annotations that trigger validation}. In case of validation failure, a
 * {@link MethodArgumentNotValidException} is raised and a 400 response status code returned if the
 * {@link cn.taketoday.web.handler.SimpleHandlerExceptionHandler}
 * is configured.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/28 15:32
 */
public class RequestPartMethodArgumentResolver extends AbstractMessageConverterParameterResolver {

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
   * <li>of type {@code jakarta.servlet.http.Part} unless annotated with
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
      return MultipartResolutionDelegate.isMultipartArgument(parameter.getParameter().nestedIfOptional());
    }
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    MethodParameter parameter = resolvable.getParameter();
    RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
    boolean isRequired = ((requestPart == null || requestPart.required()) && !parameter.isOptional());

    String name = getPartName(parameter, requestPart);
    parameter = parameter.nestedIfOptional();
    Object arg = null;

    Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, context);
    if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
      arg = mpArg;
    }
    else {
      try {
        var inputMessage = new RequestPartServletServerHttpRequest(context, name);
        arg = readWithMessageConverters(inputMessage, parameter, parameter.getNestedGenericParameterType());
        BindingContext binderFactory = context.getBindingContext();
        if (binderFactory != null) {
          WebDataBinder binder = binderFactory.createBinder(context, arg, name);
          if (arg != null) {
            validateIfApplicable(binder, parameter);
            if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
              throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
            }
          }
          binderFactory.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
        }
      }
      catch (MissingRequestPartException | MultipartException ex) {
        if (isRequired) {
          throw ex;
        }
      }
    }

    if (arg == null && isRequired) {
      if (!context.isMultipart()) {
        throw new MultipartException("Current request is not a multipart request");
      }
      else {
        throw new MissingRequestPartException(name);
      }
    }
    return adaptArgumentIfNecessary(arg, parameter);
  }

  private String getPartName(MethodParameter methodParam, @Nullable RequestPart requestPart) {
    String partName = requestPart != null ? requestPart.name() : "";
    if (partName.isEmpty()) {
      partName = methodParam.getParameterName();
      if (partName == null) {
        throw new IllegalArgumentException("Request part name for argument type [" +
                methodParam.getNestedParameterType().getName() +
                "] not specified, and parameter name information not found in class file either.");
      }
    }
    return partName;
  }

  @Override
  void closeStreamIfNecessary(InputStream body) {
    // RequestPartServletServerHttpRequest exposes individual part streams,
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
