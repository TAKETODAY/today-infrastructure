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

package infra.validation.beanvalidation;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import infra.aop.ProxyMethodInvocation;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.SmartFactoryBean;
import infra.core.MethodParameter;
import infra.core.OrderedSupport;
import infra.core.ReactiveAdapter;
import infra.core.ReactiveAdapterRegistry;
import infra.core.ReactiveStreams;
import infra.core.annotation.AnnotationUtils;
import infra.lang.Assert;
import infra.lang.VisibleForTesting;
import infra.util.ReflectionUtils;
import infra.validation.BeanPropertyBindingResult;
import infra.validation.Errors;
import infra.validation.annotation.Validated;
import infra.validation.method.MethodValidationException;
import infra.validation.method.MethodValidationResult;
import infra.validation.method.ParameterErrors;
import infra.validation.method.ParameterValidationResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * An AOP Alliance {@link MethodInterceptor} implementation that delegates to a
 * JSR-303 provider for performing method-level validation on annotated methods.
 *
 * <p>Applicable methods have JSR-303 constraint annotations on their parameters
 * and/or on their return value (in the latter case specified at the method level,
 * typically as inline annotation).
 *
 * <p>E.g.: {@code public @NotNull Object myValidMethod(@NotNull String arg1, @Max(10) int arg2)}
 *
 * <p>Validation groups can be specified through Framework's {@link Validated} annotation
 * at the type level of the containing target class, applying to all public service methods
 * of that class. By default, JSR-303 will validate against its default group only.
 *
 * <p>this functionality requires a Bean Validation 1.1+ provider.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MethodValidationPostProcessor
 * @see ExecutableValidator
 * @since 4.0
 */
public class MethodValidationInterceptor extends OrderedSupport implements MethodInterceptor {

  private final MethodValidationAdapter delegate;

  @VisibleForTesting
  final boolean adaptViolations;

  /**
   * Create a new MethodValidationInterceptor using a default JSR-303 validator underneath.
   */
  public MethodValidationInterceptor() {
    this(new MethodValidationAdapter(), false);
  }

  /**
   * Create a new MethodValidationInterceptor using the given JSR-303 ValidatorFactory.
   *
   * @param validatorFactory the JSR-303 ValidatorFactory to use
   */
  public MethodValidationInterceptor(ValidatorFactory validatorFactory) {
    this(new MethodValidationAdapter(validatorFactory), false);
  }

  /**
   * Create a new MethodValidationInterceptor using the given JSR-303 Validator.
   *
   * @param validator the JSR-303 Validator to use
   */
  public MethodValidationInterceptor(Validator validator) {
    this(validator, false);
  }

  /**
   * Create a new MethodValidationInterceptor for the supplied
   * (potentially lazily initialized) Validator.
   *
   * @param validator a Supplier for the Validator to use
   */
  public MethodValidationInterceptor(Supplier<Validator> validator) {
    this(validator, false);
  }

  /**
   * Create a new MethodValidationInterceptor using a default JSR-303 validator underneath.
   */
  public MethodValidationInterceptor(boolean adaptViolations) {
    this(new MethodValidationAdapter(), adaptViolations);
  }

  /**
   * Create a new MethodValidationInterceptor for the supplied
   * (potentially lazily initialized) Validator.
   *
   * @param validator the JSR-303 Validator to use
   * @param adaptViolations whether to adapt {@link ConstraintViolation}s, and
   * if {@code true}, raise {@link MethodValidationException}, of if
   * {@code false} raise {@link ConstraintViolationException} instead
   */
  public MethodValidationInterceptor(Validator validator, boolean adaptViolations) {
    this(new MethodValidationAdapter(validator), adaptViolations);
  }

  /**
   * Create a new MethodValidationInterceptor for the supplied
   * (potentially lazily initialized) Validator.
   *
   * @param validator a Supplier for the Validator to use
   * @param adaptViolations whether to adapt {@link ConstraintViolation}s, and
   * if {@code true}, raise {@link MethodValidationException}, of if
   * {@code false} raise {@link ConstraintViolationException} instead
   */
  public MethodValidationInterceptor(Supplier<Validator> validator, boolean adaptViolations) {
    this(new MethodValidationAdapter(validator), adaptViolations);
  }

  private MethodValidationInterceptor(MethodValidationAdapter validationAdapter, boolean adaptViolations) {
    this.delegate = validationAdapter;
    this.adaptViolations = adaptViolations;
  }

  @Override
  @Nullable
  public Object invoke(MethodInvocation invocation) throws Throwable {
    // Avoid Validator invocation on FactoryBean.getObjectType/isSingleton
    if (isFactoryBeanMetadataMethod(invocation.getMethod())) {
      return invocation.proceed();
    }

    Object target = getTarget(invocation);
    Method method = invocation.getMethod();
    @Nullable Object[] arguments = invocation.getArguments();
    Class<?>[] groups = determineValidationGroups(invocation);

    if (ReactiveStreams.reactorPresent) {
      arguments = ReactorValidationHelper.insertAsyncValidation(
              delegate.getValidatorAdapter(), this.adaptViolations, target, method, arguments);
    }

    Set<ConstraintViolation<Object>> violations;

    if (this.adaptViolations) {
      this.delegate.applyArgumentValidation(target, method, null, arguments, groups);
    }
    else {
      violations = this.delegate.invokeValidatorForArguments(target, method, arguments, groups);
      if (!violations.isEmpty()) {
        throw new ConstraintViolationException(violations);
      }
    }

    Object returnValue = invocation.proceed();

    if (this.adaptViolations) {
      this.delegate.applyReturnValueValidation(target, method, null, returnValue, groups);
    }
    else {
      violations = this.delegate.invokeValidatorForReturnValue(target, method, returnValue, groups);
      if (!violations.isEmpty()) {
        throw new ConstraintViolationException(violations);
      }
    }

    return returnValue;
  }

  private static Object getTarget(MethodInvocation invocation) {
    Object target = invocation.getThis();
    if (target == null && invocation instanceof ProxyMethodInvocation methodInvocation) {
      // Allow validation for AOP proxy without a target
      target = methodInvocation.getProxy();
    }
    Assert.state(target != null, "Target is required");
    return target;
  }

  private boolean isFactoryBeanMetadataMethod(Method method) {
    Class<?> clazz = method.getDeclaringClass();

    // Call from interface-based proxy handle, allowing for an efficient check?
    if (clazz.isInterface()) {
      return (clazz == FactoryBean.class || clazz == SmartFactoryBean.class)
              && !method.getName().equals("getObject");
    }

    // Call from CGLIB proxy handle, potentially implementing a FactoryBean method?
    Class<?> factoryBeanType = null;
    if (SmartFactoryBean.class.isAssignableFrom(clazz)) {
      factoryBeanType = SmartFactoryBean.class;
    }
    else if (FactoryBean.class.isAssignableFrom(clazz)) {
      factoryBeanType = FactoryBean.class;
    }
    return factoryBeanType != null
            && !method.getName().equals("getObject")
            && ReflectionUtils.hasMethod(factoryBeanType, method);
  }

  /**
   * Determine the validation groups to validate against for the given method invocation.
   * <p>Default are the validation groups as specified in the {@link Validated} annotation
   * on the method, or on the containing target class of the method, or for an AOP proxy
   * without a target (with all behavior in advisors), also check on proxied interfaces.
   *
   * @param invocation the current MethodInvocation
   * @return the applicable validation groups as a Class array
   */
  protected Class<?>[] determineValidationGroups(MethodInvocation invocation) {
    Object target = getTarget(invocation);
    return delegate.determineValidationGroups(target, invocation.getMethod());
  }

  /**
   * Helper class to decorate reactive arguments with async validation.
   */
  private static final class ReactorValidationHelper {

    private static final ReactiveAdapterRegistry reactiveAdapterRegistry =
            ReactiveAdapterRegistry.getSharedInstance();

    @Nullable
    static Object[] insertAsyncValidation(InfraValidatorAdapter validatorAdapter,
            boolean adaptViolations, Object target, Method method, @Nullable Object[] arguments) {

      for (int i = 0; i < method.getParameterCount(); i++) {
        if (arguments[i] == null) {
          continue;
        }
        Class<?> parameterType = method.getParameterTypes()[i];
        ReactiveAdapter reactiveAdapter = reactiveAdapterRegistry.getAdapter(parameterType);
        if (reactiveAdapter == null || reactiveAdapter.isNoValue()) {
          continue;
        }
        Class<?>[] groups = determineValidationGroups(method.getParameters()[i]);
        if (groups == null) {
          continue;
        }
        MethodParameter param = new MethodParameter(method, i);
        arguments[i] = (reactiveAdapter.isMultiValue() ?
                Flux.from(reactiveAdapter.toPublisher(arguments[i])).doOnNext(value ->
                        validate(validatorAdapter, adaptViolations, target, method, param, value, groups)) :
                Mono.from(reactiveAdapter.toPublisher(arguments[i])).doOnNext(value ->
                        validate(validatorAdapter, adaptViolations, target, method, param, value, groups)));
      }
      return arguments;
    }

    private static Class<?> @Nullable [] determineValidationGroups(Parameter parameter) {
      Validated validated = AnnotationUtils.findAnnotation(parameter, Validated.class);
      if (validated != null) {
        return validated.value();
      }
      Valid valid = AnnotationUtils.findAnnotation(parameter, Valid.class);
      if (valid != null) {
        return new Class<?>[0];
      }
      return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> void validate(InfraValidatorAdapter validatorAdapter, boolean adaptViolations,
            Object target, Method method, MethodParameter parameter, Object argument, Class<?>[] groups) {

      if (adaptViolations) {
        Errors errors = new BeanPropertyBindingResult(argument, argument.getClass().getSimpleName());
        validatorAdapter.validate(argument, errors);
        if (errors.hasErrors()) {
          ParameterErrors paramErrors = new ParameterErrors(parameter, argument, errors, null, null, null);
          List<ParameterValidationResult> results = Collections.singletonList(paramErrors);
          throw new MethodValidationException(MethodValidationResult.create(target, method, results));
        }
      }
      else {
        Set<ConstraintViolation<T>> violations = validatorAdapter.validate((T) argument, groups);
        if (!violations.isEmpty()) {
          throw new ConstraintViolationException(violations);
        }
      }
    }
  }

}
