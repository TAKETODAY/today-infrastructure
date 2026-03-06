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

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import infra.core.Conventions;
import infra.core.MethodParameter;
import infra.core.ParameterNameDiscoverer;
import infra.validation.BindingResult;
import infra.validation.MessageCodesResolver;
import infra.validation.SmartValidator;
import infra.validation.annotation.ValidationAnnotationUtils;
import infra.validation.beanvalidation.MethodValidationAdapter;
import infra.validation.method.MethodValidationResult;
import infra.validation.method.MethodValidator;
import infra.validation.method.ParameterErrors;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestPart;
import infra.web.bind.support.ConfigurableWebBindingInitializer;
import infra.web.bind.support.WebBindingInitializer;
import jakarta.validation.Validator;

/**
 * {@link MethodValidator} that
 * uses Bean Validation to validate {@code @RequestMapping} method arguments.
 *
 * <p>Handles validation results by populating {@link BindingResult} method
 * arguments with errors from {@link MethodValidationResult#getBeanResults()
 * beanResults}. Also, helps to determine parameter names for
 * {@code @ModelAttribute} and {@code @RequestBody} parameters.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class HandlerMethodValidator implements MethodValidator {

  private static final MethodValidationAdapter.ObjectNameResolver objectNameResolver = new WebObjectNameResolver();

  private final MethodValidationAdapter validationAdapter;

  private final Predicate<MethodParameter> modelAttributePredicate;

  private final Predicate<MethodParameter> requestParamPredicate;

  private HandlerMethodValidator(MethodValidationAdapter validationAdapter,
          Predicate<MethodParameter> modelAttributePredicate, Predicate<MethodParameter> requestParamPredicate) {

    this.validationAdapter = validationAdapter;
    this.modelAttributePredicate = modelAttributePredicate;
    this.requestParamPredicate = requestParamPredicate;
  }

  @Override
  public Class<?>[] determineValidationGroups(Object target, Method method) {
    return ValidationAnnotationUtils.determineValidationGroups(target, method);
  }

  @Override
  public void applyArgumentValidation(
          Object target, Method method, MethodParameter @Nullable [] parameters,
          @Nullable Object[] arguments, Class<?>[] groups) {

    MethodValidationResult result = validateArguments(target, method, parameters, arguments, groups);
    if (!result.hasErrors()) {
      return;
    }

    if (!result.getBeanResults().isEmpty()) {
      int bindingResultCount = 0;
      for (ParameterErrors errors : result.getBeanResults()) {
        for (Object arg : arguments) {
          if (arg instanceof BindingResult bindingResult) {
            if (bindingResult.getObjectName().equals(errors.getObjectName())) {
              bindingResult.addAllErrors(errors);
              bindingResultCount++;
              break;
            }
          }
        }
      }
      if (result.getParameterValidationResults().size() == bindingResultCount) {
        return;
      }
    }

    throw new HandlerMethodValidationException(
            result, this.modelAttributePredicate, this.requestParamPredicate);
  }

  @Override
  public MethodValidationResult validateArguments(Object target, Method method,
          MethodParameter @Nullable [] parameters, @Nullable Object[] arguments, Class<?>[] groups) {

    return this.validationAdapter.validateArguments(target, method, parameters, arguments, groups);
  }

  @Override
  public void applyReturnValueValidation(Object target, Method method, @Nullable MethodParameter returnType,
          @Nullable Object returnValue, Class<?>[] groups) {

    MethodValidationResult result = validateReturnValue(target, method, returnType, returnValue, groups);
    if (result.hasErrors()) {
      throw new HandlerMethodValidationException(result);
    }
  }

  @Override
  public MethodValidationResult validateReturnValue(Object target, Method method,
          @Nullable MethodParameter returnType, @Nullable Object returnValue, Class<?>[] groups) {

    return this.validationAdapter.validateReturnValue(target, method, returnType, returnValue, groups);
  }

  /**
   * Static factory method to create a {@link HandlerMethodValidator} when Bean
   * Validation is enabled for use via {@link ConfigurableWebBindingInitializer},
   * for example in Web MVC config.
   */
  public static @Nullable MethodValidator from(@Nullable WebBindingInitializer initializer, @Nullable ParameterNameDiscoverer paramNameDiscoverer,
          Predicate<MethodParameter> modelAttributePredicate, Predicate<MethodParameter> requestParamPredicate) {

    if (initializer instanceof ConfigurableWebBindingInitializer ci) {
      Validator validator = getValidator(ci);
      if (validator != null) {
        MethodValidationAdapter adapter = new MethodValidationAdapter(validator);
        adapter.setObjectNameResolver(objectNameResolver);
        if (paramNameDiscoverer != null) {
          adapter.setParameterNameDiscoverer(paramNameDiscoverer);
        }
        MessageCodesResolver codesResolver = ci.getMessageCodesResolver();
        if (codesResolver != null) {
          adapter.setMessageCodesResolver(codesResolver);
        }
        return new HandlerMethodValidator(adapter, modelAttributePredicate, requestParamPredicate);
      }
    }
    return null;
  }

  private static @Nullable Validator getValidator(ConfigurableWebBindingInitializer initializer) {
    if (initializer.getValidator() instanceof Validator validator) {
      return validator;
    }
    if (initializer.getValidator() instanceof SmartValidator smartValidator) {
      return smartValidator.unwrap(Validator.class);
    }
    return null;
  }

  /**
   * ObjectNameResolver for web controller methods.
   */
  private static final class WebObjectNameResolver implements MethodValidationAdapter.ObjectNameResolver {

    @Override
    public String resolveName(MethodParameter param, @Nullable Object value) {
      if (param.hasParameterAnnotation(RequestBody.class) || param.hasParameterAnnotation(RequestPart.class)) {
        return Conventions.getVariableNameForParameter(param);
      }
      else {
        return param.getParameterIndex() != -1 ?
                ModelHandler.getNameForParameter(param) :
                ModelHandler.getNameForReturnValue(value, param);
      }
    }
  }

}
