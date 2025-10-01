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

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import infra.beans.BeanUtils;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.core.MethodParameter;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.StringUtils;
import infra.web.HandlerMatchingMetadata;
import infra.web.RequestContext;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RequestPart;
import infra.web.bind.MissingRequestParameterException;
import infra.web.bind.MultipartException;
import infra.web.bind.WebDataBinder;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.handler.method.support.UriComponentsContributor;
import infra.web.multipart.Multipart;
import infra.web.multipart.MultipartFile;
import infra.web.multipart.MultipartRequest;
import infra.web.util.UriComponentsBuilder;

/**
 * Resolves method arguments annotated with @{@link RequestParam}, arguments of
 * type {@link MultipartFile} in conjunction with {@link MultipartRequest}
 * abstraction. This resolver can also be created in default
 * resolution mode in which simple types (int, long, etc.) not annotated with
 * {@link RequestParam @RequestParam} are also treated as request parameters with
 * the parameter name derived from the argument name.
 *
 * <p>If the method parameter type is {@link Map}, the name specified in the
 * annotation is used to resolve the request parameter String value. The value is
 * then converted to a {@link Map} via type conversion assuming a suitable
 * {@link Converter} or {@link PropertyEditor} has been registered.
 * Or if a request parameter name is not specified the
 * {@link RequestParamMapMethodArgumentResolver} is used instead to provide
 * access to all request parameters in the form of a map.
 *
 * <p>A {@link WebDataBinder} is invoked to apply type conversion to resolved request
 * header values that don't yet match the method parameter type.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestParamMapMethodArgumentResolver
 * @since 4.0 2022/4/28 13:57
 */
public class RequestParamMethodArgumentResolver extends AbstractNamedValueResolvingStrategy implements UriComponentsContributor {

  private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);

  private final boolean useDefaultResolution;

  /**
   * Create a new {@link RequestParamMethodArgumentResolver} instance.
   *
   * @param useDefaultResolution in default resolution mode a method argument
   * that is a simple type, as defined in {@link BeanUtils#isSimpleProperty},
   * is treated as a request parameter even if it isn't annotated, the
   * request parameter name is derived from the method parameter name.
   */
  public RequestParamMethodArgumentResolver(boolean useDefaultResolution) {
    this.useDefaultResolution = useDefaultResolution;
  }

  /**
   * Create a new {@link RequestParamMethodArgumentResolver} instance.
   *
   * @param beanFactory a bean factory used for resolving  ${...} placeholder
   * and #{...} SpEL expressions in default values, or {@code null} if default
   * values are not expected to contain expressions
   * @param useDefaultResolution in default resolution mode a method argument
   * that is a simple type, as defined in {@link BeanUtils#isSimpleProperty},
   * is treated as a request parameter even if it isn't annotated, the
   * request parameter name is derived from the method parameter name.
   */
  public RequestParamMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory, boolean useDefaultResolution) {
    super(beanFactory);
    this.useDefaultResolution = useDefaultResolution;
  }

  /**
   * Supports the following:
   * <ul>
   * <li>@RequestParam-annotated method arguments.
   * This excludes {@link Map} params where the annotation does not specify a name.
   * See {@link RequestParamMapMethodArgumentResolver} instead for such params.
   * <li>Arguments of type {@link MultipartFile} unless annotated with @{@link RequestPart}.
   * <li>Arguments of type {@code Part} unless annotated with @{@link RequestPart}.
   * <li>In default resolution mode, simple type arguments even if not with @{@link RequestParam}.
   * </ul>
   */
  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    if (parameter.hasParameterAnnotation(RequestParam.class)) {
      if (Map.class.isAssignableFrom(parameter.getParameterType())) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        return requestParam != null && StringUtils.hasText(requestParam.name());
      }
      else {
        return true;
      }
    }
    else {
      if (parameter.hasParameterAnnotation(RequestPart.class)) {
        return false;
      }
      if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
        return true;
      }
      else if (useDefaultResolution) {
        return BeanUtils.isSimpleProperty(parameter.getParameterType());
      }
      else {
        return false;
      }
    }
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return supportsParameter(resolvable.getParameter());
  }

  @Nullable
  @Override
  protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext request) throws Exception {
    MethodParameter parameter = resolvable.getParameter();
    Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, request);
    if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
      return mpArg;
    }

    Object arg = null;
    String[] paramValues = request.getParameters(name);
    if (paramValues == null) {
      paramValues = request.getParameters(name + "[]");
    }
    if (paramValues != null) {
      arg = paramValues.length == 1 ? paramValues[0] : paramValues;
    }
    else {
      // fallback path-variable, can resolve a none-annotated param
      HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
      if (matchingMetadata != null) {
        arg = matchingMetadata.getUriVariable(name);
      }
    }

    if (arg == null) {
      if (request.isMultipart()) {
        List<Multipart> parts = request.multipartRequest().multipartData(name);
        if (parts != null) {
          arg = parts.size() == 1 ? parts.get(0) : parts;
        }
      }
    }

    return arg;
  }

  @Override
  protected void handleMissingValue(String name, MethodParameter parameter, RequestContext request) throws Exception {
    handleMissingValueInternal(name, parameter, request, false);
  }

  @Override
  protected void handleMissingValueAfterConversion(String name, MethodParameter parameter, RequestContext request) throws Exception {
    handleMissingValueInternal(name, parameter, request, true);
  }

  protected void handleMissingValueInternal(String name,
          MethodParameter parameter, RequestContext request, boolean missingAfterConversion) throws Exception {

    if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
      if (!request.isMultipart()) {
        throw new MultipartException("Current request is not a multipart request");
      }
      else {
        throw new MissingRequestPartException(name);
      }
    }
    else {
      throw new MissingRequestParameterException(name, parameter, missingAfterConversion);
    }
  }

  @Override
  public void contributeMethodArgument(MethodParameter parameter, @Nullable Object value,
          UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

    Class<?> paramType = parameter.getNestedParameterType();
    if (Map.class.isAssignableFrom(paramType)) {
      return;
    }

    RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
    String name = requestParam != null && StringUtils.isNotEmpty(requestParam.name())
            ? requestParam.name() : parameter.getParameterName();
    Assert.state(name != null, "Unresolvable parameter name");

    if (value instanceof Optional<?> optional) {
      value = optional.orElse(null);
    }

    if (value == null) {
      if (requestParam != null &&
              (!requestParam.required() || !requestParam.defaultValue().equals(Constant.DEFAULT_NONE))) {
        return;
      }
      builder.queryParam(name);
    }
    else if (value instanceof Collection<?> collection) {
      for (Object element : collection) {
        element = formatUriValue(conversionService, TypeDescriptor.nested(parameter, 1), element);
        builder.queryParam(name, element);
      }
    }
    else {
      builder.queryParam(name, formatUriValue(conversionService, new TypeDescriptor(parameter), value));
    }
  }

  @Nullable
  protected String formatUriValue(@Nullable ConversionService cs,
          @Nullable TypeDescriptor sourceType, @Nullable Object value) {

    if (value == null) {
      return null;
    }
    else if (value instanceof String) {
      return (String) value;
    }
    else if (cs != null) {
      return (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
    }
    else {
      return value.toString();
    }
  }

}
