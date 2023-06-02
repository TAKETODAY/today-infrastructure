/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.validation.beanvalidation;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.util.Set;

import cn.taketoday.aop.ProxyMethodInvocation;
import cn.taketoday.aop.framework.AopProxyUtils;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.SmartFactoryBean;
import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.validation.annotation.Validated;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
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
 * @see MethodValidationPostProcessor
 * @see ExecutableValidator
 * @since 4.0
 */
public class MethodValidationInterceptor extends OrderedSupport implements MethodInterceptor {

  private final Validator validator;

  /**
   * Create a new MethodValidationInterceptor using a default JSR-303 validator underneath.
   */
  public MethodValidationInterceptor() {
    this(Validation.buildDefaultValidatorFactory());
  }

  /**
   * Create a new MethodValidationInterceptor using the given JSR-303 ValidatorFactory.
   *
   * @param validatorFactory the JSR-303 ValidatorFactory to use
   */
  public MethodValidationInterceptor(ValidatorFactory validatorFactory) {
    this(validatorFactory.getValidator());
  }

  /**
   * Create a new MethodValidationInterceptor using the given JSR-303 Validator.
   *
   * @param validator the JSR-303 Validator to use
   */
  public MethodValidationInterceptor(Validator validator) {
    this.validator = validator;
    setOrder(HIGHEST_PRECEDENCE); // highest
  }

  @Override
  @Nullable
  public Object invoke(MethodInvocation invocation) throws Throwable {
    // Avoid Validator invocation on FactoryBean.getObjectType/isSingleton
    if (isFactoryBeanMetadataMethod(invocation.getMethod())) {
      return invocation.proceed();
    }

    Class<?>[] groups = determineValidationGroups(invocation);

    // Standard Bean Validation 1.1 API
    ExecutableValidator execVal = this.validator.forExecutables();
    Method methodToValidate = invocation.getMethod();
    Set<ConstraintViolation<Object>> result;

    Object target = invocation.getThis();
    if (target == null && invocation instanceof ProxyMethodInvocation methodInvocation) {
      // Allow validation for AOP proxy without a target
      target = methodInvocation.getProxy();
    }
    Assert.state(target != null, "Target must not be null");

    try {
      result = execVal.validateParameters(target, methodToValidate, invocation.getArguments(), groups);
    }
    catch (IllegalArgumentException ex) {
      // Probably a generic type mismatch between interface and impl as reported in SPR-12237 / HV-1011
      // Let's try to find the bridged method on the implementation class...
      methodToValidate = BridgeMethodResolver.findBridgedMethod(
              ReflectionUtils.getMostSpecificMethod(invocation.getMethod(), target.getClass()));
      result = execVal.validateParameters(target, methodToValidate, invocation.getArguments(), groups);
    }
    if (!result.isEmpty()) {
      throw new ConstraintViolationException(result);
    }

    Object returnValue = invocation.proceed();

    result = execVal.validateReturnValue(target, methodToValidate, returnValue, groups);
    if (!result.isEmpty()) {
      throw new ConstraintViolationException(result);
    }

    return returnValue;
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
    Validated validatedAnn = AnnotationUtils.findAnnotation(invocation.getMethod(), Validated.class);
    if (validatedAnn == null) {
      Object target = invocation.getThis();
      if (target != null) {
        validatedAnn = AnnotationUtils.findAnnotation(target.getClass(), Validated.class);
      }
      else if (invocation instanceof ProxyMethodInvocation methodInvocation) {
        Object proxy = methodInvocation.getProxy();
        if (AopUtils.isAopProxy(proxy)) {
          for (Class<?> type : AopProxyUtils.proxiedUserInterfaces(proxy)) {
            validatedAnn = AnnotationUtils.findAnnotation(type, Validated.class);
            if (validatedAnn != null) {
              break;
            }
          }
        }
      }
    }
    return validatedAnn != null ? validatedAnn.value() : Constant.EMPTY_CLASSES;
  }

}
