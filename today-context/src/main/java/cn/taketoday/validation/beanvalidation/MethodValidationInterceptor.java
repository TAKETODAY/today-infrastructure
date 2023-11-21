/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.validation.beanvalidation;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.aop.ProxyMethodInvocation;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.SmartFactoryBean;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.validation.annotation.Validated;
import cn.taketoday.validation.method.MethodValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;

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

  private final boolean adaptViolations;

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
    Object[] arguments = invocation.getArguments();
    Class<?>[] groups = determineValidationGroups(invocation);

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
      this.delegate.applyReturnValueValidation(target, method, null, arguments, groups);
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

}
