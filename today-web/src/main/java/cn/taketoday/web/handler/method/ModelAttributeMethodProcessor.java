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

package cn.taketoday.web.handler.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.validation.BindException;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.DataBinder;
import cn.taketoday.validation.Errors;
import cn.taketoday.validation.SmartValidator;
import cn.taketoday.validation.Validator;
import cn.taketoday.validation.annotation.ValidationAnnotationUtils;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.RequestContextDataBinder;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.MultipartRequest;

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
    BindingContext bindingContext = context.getBindingContext();
    Assert.state(bindingContext != null, "No binding context");

    MethodParameter parameter = resolvable.getParameter();
    String name = ModelFactory.getNameForParameter(parameter);

    ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
    if (ann != null) {
      bindingContext.setBinding(name, ann.binding());
    }

    Object attribute;
    BindingResult bindingResult = null;

    if (bindingContext.containsAttribute(name)) {
      attribute = bindingContext.getModel().getAttribute(name);
    }
    else {
      // Create attribute instance
      try {
        attribute = createAttribute(name, parameter, bindingContext, context);
      }
      catch (BindException ex) {
        if (isBindExceptionRequired(parameter)) {
          // No BindingResult parameter -> fail with BindException
          throw ex;
        }
        // Otherwise, expose null/empty value and associated BindingResult
        if (parameter.getParameterType() == Optional.class) {
          attribute = Optional.empty();
        }
        else {
          attribute = ex.getTarget();
        }
        bindingResult = ex.getBindingResult();
      }
    }

    if (bindingResult == null) {
      // Bean property binding and validation;
      // skipped in case of binding failure on construction.
      RequestContextDataBinder binder = bindingContext.createBinder(context, attribute, name);
      if (binder.getTarget() != null) {
        if (!bindingContext.isBindingDisabled(name)) {
          bindRequestParameters(binder, context);
        }
        validateIfApplicable(binder, parameter);
        if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
          throw new BindException(binder.getBindingResult());
        }
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

  /**
   * Extension point to create the model attribute if not found in the model,
   * with subsequent parameter binding through bean properties (unless suppressed).
   * <p>The default implementation typically uses the unique public no-arg constructor
   * if available but also handles a "primary constructor" approach for data classes:
   * It understands the JavaBeans {@code ConstructorProperties} annotation as well as
   * runtime-retained parameter names in the bytecode, associating request parameters
   * with constructor arguments by name. If no such constructor is found, the default
   * constructor will be used (even if not public), assuming subsequent bean property
   * bindings through setter methods.
   *
   * @param attributeName the name of the attribute (never {@code null})
   * @param parameter the method parameter declaration
   * @param bindingContext for creating WebDataBinder instance
   * @param request the current request
   * @return the created model attribute (never {@code null})
   * @throws BindException in case of constructor argument binding failure
   * @throws Exception in case of constructor invocation failure
   * @see #constructAttribute(Constructor, String, MethodParameter, BindingContext, RequestContext)
   */
  protected Object createAttribute(String attributeName, MethodParameter parameter,
          BindingContext bindingContext, RequestContext request) throws Throwable {

    String value = getRequestValueForAttribute(attributeName, request);
    if (value != null) {
      Object attribute = createAttributeFromRequestValue(
              value, attributeName, parameter, bindingContext, request);
      if (attribute != null) {
        return attribute;
      }
    }

    MethodParameter nestedParameter = parameter.nestedIfOptional();
    Class<?> clazz = nestedParameter.getNestedParameterType();

    Constructor<?> ctor = BeanUtils.obtainConstructor(clazz);
    Object attribute = constructAttribute(ctor, attributeName, parameter, bindingContext, request);
    if (parameter != nestedParameter) {
      attribute = Optional.of(attribute);
    }
    return attribute;
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
   * Construct a new attribute instance with the given constructor.
   * <p>Called from
   * {@link #createAttribute(String, MethodParameter, BindingContext, RequestContext)}
   * after constructor resolution.
   *
   * @param ctor the constructor to use
   * @param attributeName the name of the attribute (never {@code null})
   * @param binderFactory for creating WebDataBinder instance
   * @param request the current request
   * @return the created model attribute (never {@code null})
   * @throws BindException in case of constructor argument binding failure
   * @throws Exception in case of constructor invocation failure
   */
  protected Object constructAttribute(Constructor<?> ctor, String attributeName, MethodParameter parameter,
          BindingContext binderFactory, RequestContext request) throws Throwable {

    if (ctor.getParameterCount() == 0) {
      // A single default constructor -> clearly a standard JavaBeans arrangement.
      return BeanUtils.newInstance(ctor);
    }

    // A single data class constructor -> resolve constructor arguments from request parameters.
    String[] paramNames = BeanUtils.getParameterNames(ctor);
    Class<?>[] paramTypes = ctor.getParameterTypes();
    Object[] args = new Object[paramTypes.length];
    WebDataBinder binder = binderFactory.createBinder(request, null, attributeName);
    String fieldDefaultPrefix = binder.getFieldDefaultPrefix();
    String fieldMarkerPrefix = binder.getFieldMarkerPrefix();
    boolean bindingFailure = false;
    HashSet<String> failedParams = new HashSet<>(4);

    for (int i = 0; i < paramNames.length; i++) {
      String paramName = paramNames[i];
      Class<?> paramType = paramTypes[i];
      String[] arrayValue = request.getParameters(paramName);

      Object value = null;

      if (arrayValue != null && arrayValue.length == 1) {
        value = arrayValue[0];
      }

      if (value == null) {
        if (fieldDefaultPrefix != null) {
          value = request.getParameter(fieldDefaultPrefix + paramName);
        }
        if (value == null) {
          if (fieldMarkerPrefix != null && request.getParameter(fieldMarkerPrefix + paramName) != null) {
            value = binder.getEmptyValue(paramType);
          }
          else {
            value = resolveConstructorArgument(paramName, paramType, request);
          }
        }
      }

      try {
        MethodParameter methodParam = new FieldAwareConstructorParameter(ctor, i, paramName);
        if (value == null && methodParam.isOptional()) {
          args[i] = methodParam.getParameterType() == Optional.class ? Optional.empty() : null;
        }
        else {
          args[i] = binder.convertIfNecessary(value, paramType, methodParam);
        }
      }
      catch (TypeMismatchException ex) {
        ex.initPropertyName(paramName);
        args[i] = null;
        failedParams.add(paramName);
        binder.getBindingResult().recordFieldValue(paramName, paramType, value);
        binder.getBindingErrorProcessor().processPropertyAccessException(ex, binder.getBindingResult());
        bindingFailure = true;
      }
    }

    if (bindingFailure) {
      BindingResult result = binder.getBindingResult();
      for (int i = 0; i < paramNames.length; i++) {
        String paramName = paramNames[i];
        if (!failedParams.contains(paramName)) {
          Object value = args[i];
          result.recordFieldValue(paramName, paramTypes[i], value);
          validateValueIfApplicable(binder, parameter, ctor.getDeclaringClass(), paramName, value);
        }
      }
      if (!parameter.isOptional()) {
        try {
          Object target = BeanUtils.newInstance(ctor, args);
          throw new BindException(result) {
            @Override
            public Object getTarget() {
              return target;
            }
          };
        }
        catch (BeanInstantiationException ex) {
          // swallow and proceed without target instance
        }
      }
      throw new BindException(result);
    }

    return BeanUtils.newInstance(ctor, args);
  }

  /**
   * Extension point to bind the request to the target object.
   *
   * @param binder the data binder instance to use for the binding
   * @param request the current request
   */
  protected void bindRequestParameters(RequestContextDataBinder binder, RequestContext request) {
    binder.bind(request);
  }

  @Nullable
  public Object resolveConstructorArgument(String paramName, Class<?> paramType, RequestContext request) throws Exception {
    if (request.isMultipart()) {
      MultipartRequest multipartRequest = request.getMultipartRequest();
      MultiValueMap<String, MultipartFile> multipartFiles = multipartRequest.getMultipartFiles();
      if (CollectionUtils.isNotEmpty(multipartFiles)) {
        List<MultipartFile> files = multipartFiles.get(paramName);
        if (CollectionUtils.isNotEmpty(files)) {
          return files.size() == 1 ? files.get(0) : files;
        }
      }
    }

    return getUriVariables(request).get(paramName);
  }

  /**
   * Validate the model attribute if applicable.
   * <p>The default implementation checks for {@code @jakarta.validation.Valid},
   * Spring's {@link cn.taketoday.validation.annotation.Validated},
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
   * Validate the specified candidate value if applicable.
   * <p>The default implementation checks for {@code @jakarta.validation.Valid},
   * Spring's {@link cn.taketoday.validation.annotation.Validated},
   * and custom annotations whose name starts with "Valid".
   *
   * @param binder the DataBinder to be used
   * @param parameter the method parameter declaration
   * @param targetType the target type
   * @param fieldName the name of the field
   * @param value the candidate value
   * @see #validateIfApplicable(WebDataBinder, MethodParameter)
   * @see SmartValidator#validateValue(Class, String, Object, Errors, Object...)
   */
  protected void validateValueIfApplicable(WebDataBinder binder, MethodParameter parameter,
          Class<?> targetType, String fieldName, @Nullable Object value) {

    for (Annotation ann : parameter.getParameterAnnotations()) {
      Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
      if (validationHints != null) {
        for (Validator validator : binder.getValidators()) {
          if (validator instanceof SmartValidator smartValidator) {
            try {
              smartValidator.validateValue(targetType, fieldName, value,
                      binder.getBindingResult(), validationHints);
            }
            catch (IllegalArgumentException ex) {
              // No corresponding field on the target class...
            }
          }
        }
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
  public void handleReturnValue(RequestContext context, HandlerMethod handler, @Nullable Object returnValue) throws Exception {
    if (returnValue != null) {
      BindingContext bindingContext = context.getBindingContext();
      String name = ModelFactory.getNameForReturnValue(returnValue, handler);
      bindingContext.addAttribute(name, returnValue);
    }
  }

  /**
   * {@link MethodParameter} subclass which detects field annotations as well.
   */
  private static class FieldAwareConstructorParameter extends MethodParameter {

    private final String parameterName;

    @Nullable
    private volatile Annotation[] combinedAnnotations;

    public FieldAwareConstructorParameter(Constructor<?> constructor, int parameterIndex, String parameterName) {
      super(constructor, parameterIndex);
      this.parameterName = parameterName;
    }

    @Override
    public Annotation[] getParameterAnnotations() {
      Annotation[] anns = this.combinedAnnotations;
      if (anns == null) {
        anns = super.getParameterAnnotations();
        try {
          Field field = getDeclaringClass().getDeclaredField(parameterName);
          Annotation[] fieldAnns = field.getAnnotations();
          if (fieldAnns.length > 0) {
            ArrayList<Annotation> merged = new ArrayList<>(anns.length + fieldAnns.length);
            CollectionUtils.addAll(merged, anns);
            for (Annotation fieldAnn : fieldAnns) {
              boolean existingType = false;
              for (Annotation ann : anns) {
                if (ann.annotationType() == fieldAnn.annotationType()) {
                  existingType = true;
                  break;
                }
              }
              if (!existingType) {
                merged.add(fieldAnn);
              }
            }
            anns = merged.toArray(new Annotation[0]);
          }
        }
        catch (NoSuchFieldException | SecurityException ex) {
          // ignore
        }
        this.combinedAnnotations = anns;
      }
      return anns;
    }

    @Override
    public String getParameterName() {
      return this.parameterName;
    }
  }

}
