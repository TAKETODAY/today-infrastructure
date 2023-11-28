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

package cn.taketoday.web.handler.method;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.BindException;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.DataBinder;
import cn.taketoday.validation.Errors;
import cn.taketoday.validation.SmartValidator;
import cn.taketoday.validation.annotation.ValidationAnnotationUtils;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;

/**
 * Resolve {@code @ModelAttribute} annotated method arguments and handle
 * return values from {@code @ModelAttribute} annotated methods.
 *
 * <p>Model attributes are obtained from the model or created with a default
 * constructor (and then added to the model). Once created the attribute is
 * populated via data binding to Servlet request parameters. Validation may be
 * applied if the argument is annotated with {@code @jakarta.validation.Valid}.
 * or Framework's own {@code @cn.taketoday.validation.annotation.Validated}.
 *
 * <p>When this handler is created with {@code annotationNotRequired=true}
 * any non-simple type argument and return value is regarded as a model
 * attribute with or without the presence of an {@code @ModelAttribute}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Vladislav Kisel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/26 16:25
 */
public class ModelAttributeMethodProcessor implements ParameterResolvingStrategy, HandlerMethodReturnValueHandler {

  private final boolean annotationNotRequired;

  /**
   * Class constructor.
   *
   * @param annotationNotRequired if "true", non-simple method arguments and
   * return values are considered model attributes with or without a
   * {@code @ModelAttribute} annotation
   */
  public ModelAttributeMethodProcessor(boolean annotationNotRequired) {
    this.annotationNotRequired = annotationNotRequired;
  }

  /**
   * Returns {@code true} if the parameter is annotated with
   * {@link ModelAttribute} or, if in default resolution mode, for any
   * method parameter that is not a simple type.
   */
  @Override
  public boolean supportsParameter(ResolvableMethodParameter resolvable) {
    return resolvable.hasParameterAnnotation(ModelAttribute.class)
            || (annotationNotRequired && !BeanUtils.isSimpleProperty(resolvable.getParameterType()));
  }

  /**
   * Resolve the argument from the model or if not found instantiate it with
   * its default if it is available. The model attribute is then populated
   * with request values via data binding and optionally validated
   * if {@code @java.validation.Valid} is present on the argument.
   *
   * @throws BindException if data binding and validation result in an error
   * and the next method parameter is not of type {@link Errors}
   * @throws Exception if WebDataBinder initialization fails
   */
  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
    BindingContext bindingContext = context.binding();

    MethodParameter parameter = resolvable.getParameter();
    String name = ModelHandler.getNameForParameter(parameter);

    ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
    if (ann != null) {
      bindingContext.setBinding(name, ann.binding());
    }

    Object attribute;
    BindingResult bindingResult = null;

    if (bindingContext.containsAttribute(name)) {
      attribute = bindingContext.getModel().get(name);
      if (attribute == null || ObjectUtils.unwrapOptional(attribute) == null) {
        bindingResult = bindingContext.createBinder(context, null, name).getBindingResult();
        attribute = wrapAsOptionalIfNecessary(parameter, null);
      }
    }
    else {
      try {
        // Mainly to allow subclasses alternative to create attribute
        attribute = createAttribute(name, parameter, bindingContext, context);
      }
      catch (MethodArgumentNotValidException ex) {
        if (isBindExceptionRequired(parameter)) {
          throw ex;
        }
        attribute = wrapAsOptionalIfNecessary(parameter, ex.getTarget());
        bindingResult = ex.getBindingResult();
      }
    }

    // No BindingResult yet, proceed with binding and validation
    if (bindingResult == null) {
      ResolvableType type = ResolvableType.forMethodParameter(parameter);
      WebDataBinder binder = bindingContext.createBinder(context, attribute, name, type);
      if (attribute == null) {
        constructAttribute(binder, context);
        attribute = wrapAsOptionalIfNecessary(parameter, binder.getTarget());
      }
      if (!binder.getBindingResult().hasErrors()) {
        if (!bindingContext.isBindingDisabled(name)) {
          bindRequestParameters(binder, context);
        }
        validateIfApplicable(binder, parameter);
      }
      if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
        throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
      }
      // Value type adaptation, also covering java.util.Optional
      if (!parameter.getParameterType().isInstance(attribute)) {
        attribute = binder.convertIfNecessary(binder.getTarget(), parameter.getParameterType(), parameter);
      }
      bindingResult = binder.getBindingResult();
    }

    // Add resolved attribute and BindingResult at the end of the model
    Map<String, Object> bindingResultModel = bindingResult.getModel();
    bindingContext.removeAttributes(bindingResultModel);
    bindingContext.addAllAttributes(bindingResultModel);

    return attribute;
  }

  @Nullable
  private static Object wrapAsOptionalIfNecessary(MethodParameter parameter, @Nullable Object target) {
    return parameter.getParameterType() == Optional.class ? Optional.ofNullable(target) : target;
  }

  /**
   * Extension point to create the model attribute if not found in the model,
   * with subsequent parameter binding through bean properties (unless suppressed).
   * <p>By default this method returns {@code null} in which case
   * {@link cn.taketoday.validation.DataBinder#construct} is used instead
   * to create the model attribute. The main purpose of this method then is to
   * allow to create the model attribute in some other, alternative way.
   *
   * @param attributeName the name of the attribute (never {@code null})
   * @param parameter the method parameter declaration
   * @param bindingContext for creating WebDataBinder instance
   * @param request the current request
   * @return the created model attribute, or {@code null}
   */
  @Nullable
  protected Object createAttribute(String attributeName, MethodParameter parameter,
          BindingContext bindingContext, RequestContext request) throws Throwable {

    String value = getRequestValueForAttribute(attributeName, request);
    if (value != null) {
      return createAttributeFromRequestValue(
              value, attributeName, parameter, bindingContext, request);
    }

    return null;
  }

  /**
   * Obtain a value from the request that may be used to instantiate the
   * model attribute through type conversion from String to the target type.
   * <p>The default implementation looks for the attribute name to match
   * a URI variable first and then a request parameter.
   *
   * @param attributeName the model attribute name
   * @param request the current request
   * @return the request value to try to convert, or {@code null} if none
   */
  @Nullable
  protected String getRequestValueForAttribute(String attributeName, RequestContext request) {
    String variableValue = getUriVariables(request).get(attributeName);
    if (StringUtils.hasText(variableValue)) {
      return variableValue;
    }
    String parameterValue = request.getParameter(attributeName);
    if (StringUtils.hasText(parameterValue)) {
      return parameterValue;
    }
    return null;
  }

  private Map<String, String> getUriVariables(RequestContext request) {
    HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata != null) {
      return matchingMetadata.getUriVariables();
    }
    return Collections.emptyMap();
  }

  /**
   * Create a model attribute from a String request value (e.g. URI template
   * variable, request parameter) using type conversion.
   * <p>The default implementation converts only if there a registered
   * {@link Converter} that can perform the conversion.
   *
   * @param sourceValue the source value to create the model attribute from
   * @param attributeName the name of the attribute (never {@code null})
   * @param parameter the method parameter
   * @param binderFactory for creating WebDataBinder instance
   * @param request the current request
   * @return the created model attribute, or {@code null} if no suitable
   * conversion found
   */
  @Nullable
  protected Object createAttributeFromRequestValue(String sourceValue, String attributeName,
          MethodParameter parameter, BindingContext binderFactory, RequestContext request) throws Throwable {

    DataBinder binder = binderFactory.createBinder(request, attributeName);
    ConversionService conversionService = binder.getConversionService();
    if (conversionService != null) {
      TypeDescriptor source = TypeDescriptor.valueOf(String.class);
      TypeDescriptor target = new TypeDescriptor(parameter);
      if (conversionService.canConvert(source, target)) {
        return binder.convertIfNecessary(sourceValue, parameter.getParameterType(), parameter);
      }
    }
    return null;
  }

  /**
   * Extension point to create the attribute, binding the request to constructor args.
   *
   * @param binder the data binder instance to use for the binding
   * @param request the current request
   */
  protected void constructAttribute(WebDataBinder binder, RequestContext request) {
    binder.construct(request);
  }

  /**
   * Extension point to bind the request to the target object.
   *
   * @param binder the data binder instance to use for the binding
   * @param request the current request
   */
  protected void bindRequestParameters(WebDataBinder binder, RequestContext request) {
    binder.bind(request);
  }

  /**
   * Validate the model attribute if applicable.
   * <p>The default implementation checks for {@code @jakarta.validation.Valid},
   * Infra {@link cn.taketoday.validation.annotation.Validated},
   * and custom annotations whose name starts with "Valid".
   *
   * @param binder the DataBinder to be used
   * @param parameter the method parameter declaration
   * @see WebDataBinder#validate(Object...)
   * @see SmartValidator#validate(Object, Errors, Object...)
   */
  protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
    for (Annotation ann : parameter.getParameterAnnotations()) {
      Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
      if (validationHints != null) {
        binder.validate(validationHints);
        break;
      }
    }
  }

  /**
   * Whether to raise a fatal bind exception on validation errors.
   * <p>The default implementation delegates to {@link #isBindExceptionRequired(MethodParameter)}.
   *
   * @param binder the data binder used to perform data binding
   * @param parameter the method parameter declaration
   * @return {@code true} if the next method parameter is not of type {@link Errors}
   * @see #isBindExceptionRequired(MethodParameter)
   */
  protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
    return isBindExceptionRequired(parameter);
  }

  /**
   * Whether to raise a fatal bind exception on validation errors.
   *
   * @param parameter the method parameter declaration
   * @return {@code true} if the next method parameter is not of type {@link Errors}
   */
  protected boolean isBindExceptionRequired(MethodParameter parameter) {
    int i = parameter.getParameterIndex();
    Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
    boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
    return !hasBindingResult;
  }

  /**
   * Return {@code true} if there is a method-level {@code @ModelAttribute}
   * or, in default resolution mode, for any return value type that is not
   * a simple type.
   */
  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    MethodParameter returnType = handler.getReturnType();
    return returnType.hasMethodAnnotation(ModelAttribute.class)
            || (annotationNotRequired && !BeanUtils.isSimpleProperty(returnType.getParameterType()));
  }

  /**
   * Add non-null return values to the {@link BindingContext}.
   */
  @Override
  public void handleHandlerMethodReturnValue(
          RequestContext context, HandlerMethod handler, @Nullable Object returnValue) {
    if (returnValue != null) {
      String name = ModelHandler.getNameForReturnValue(returnValue, handler);
      context.binding().addAttribute(name, returnValue);
    }
  }

}
