/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 * <p>
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.aop.support.annotation;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Joinpoint;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ExceptionUtils;

/**
 * @author TODAY 2018-11-10 11:26
 * @see Aspect
 */
public abstract class AbstractAnnotationMethodInterceptor implements Advice, MethodInterceptor, Ordered {
  /*************************************************
   * Parameter Types
   */

  public static final byte TYPE_NULL = 0x00;
  public static final byte TYPE_THROWING = 0x01;
  public static final byte TYPE_ARGUMENT = 0x02;
  public static final byte TYPE_ARGUMENTS = 0x03;
  public static final byte TYPE_RETURNING = 0x04;
  public static final byte TYPE_ANNOTATED = 0x05;
  public static final byte TYPE_JOIN_POINT = 0x06;
  public static final byte TYPE_ATTRIBUTE = 0x07;

  private final Method adviceMethod;
  private final byte[] adviceParameters;
  private final int adviceParameterLength;
  private final Class<?>[] adviceParameterTypes;

  private final BeanSupplier<?> beanSupplier;

  private volatile MethodInvoker methodInvoker;

  public AbstractAnnotationMethodInterceptor(
          Method adviceMethod, BeanFactory beanFactory, String aspectBeanName) {
    this.adviceMethod = adviceMethod;
    Assert.notNull(beanFactory, "beanFactory must not be null");
    Assert.notNull(adviceMethod, "adviceMethod must not be null");
    Assert.notNull(aspectBeanName, "aspect bean name must not be null");

    // TODO BeanSupplier outside inject
    this.beanSupplier = BeanSupplier.from(beanFactory, aspectBeanName);

    this.adviceParameterLength = adviceMethod.getParameterCount();
    this.adviceParameters = new byte[adviceParameterLength];
    this.adviceParameterTypes = adviceMethod.getParameterTypes();

    Parameter[] parameters = adviceMethod.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      adviceParameters[i] = TYPE_NULL;
      if (parameter.isAnnotationPresent(JoinPoint.class)) {
        adviceParameters[i] = TYPE_JOIN_POINT;
      }
      if (parameter.isAnnotationPresent(Argument.class)) {
        adviceParameters[i] = TYPE_ARGUMENT;
      }
      if (parameter.isAnnotationPresent(Arguments.class)) {
        adviceParameters[i] = TYPE_ARGUMENTS;
      }
      if (parameter.isAnnotationPresent(Returning.class)) {
        adviceParameters[i] = TYPE_RETURNING;
      }
      if (parameter.isAnnotationPresent(Throwing.class)) {
        adviceParameters[i] = TYPE_THROWING;
      }
      if (parameter.isAnnotationPresent(Annotated.class)) {
        adviceParameters[i] = TYPE_ANNOTATED;
      }
      if (parameter.isAnnotationPresent(Attribute.class)) {
        adviceParameters[i] = TYPE_ATTRIBUTE;
      }
    }
  }

  @Override
  public abstract Object invoke(MethodInvocation invocation) throws Throwable;

  /**
   * Invoke advice method
   *
   * @param inv Target method invocation
   * @param returnValue Target method return value
   * @param throwable Target method throws {@link Exception}
   */
  protected Object invokeAdviceMethod(
          MethodInvocation inv, Object returnValue, Throwable throwable
  ) {
    if (adviceParameterLength == 0) {
      return doInvokeAdviceMethod(null);
    }

    int idx = 0;
    Object[] args = new Object[adviceParameterLength];
    for (byte adviceParameter : this.adviceParameters) {
      switch (adviceParameter) {
        case TYPE_THROWING -> {
          if (throwable != null) {
            Class<?> parameterType = adviceParameterTypes[idx];
            throwable = ExceptionUtils.unwrapThrowable(throwable);
            if (parameterType == Throwable.class //
                    || parameterType.isAssignableFrom(throwable.getClass())) //
            {
              args[idx] = throwable;
            }
          }
        }
        case TYPE_ARGUMENT -> {
          // fix: NullPointerException
          Object[] arguments = inv.getArguments();
          if (arguments.length == 1) {
            args[idx] = arguments[0];
            break;
          }
          // for every argument matching
          for (Object argument : arguments) {
            if (argument != null && argument.getClass() == adviceParameterTypes[idx]) {
              args[idx] = argument;
              break;
            }
          }
        }
        case TYPE_ATTRIBUTE -> {
          if (inv instanceof AttributeAccessor) {
            Class<?> parameterType = adviceParameterTypes[idx];
            if (AttributeAccessor.class == parameterType
                    || MethodInvocation.class == parameterType) {
              args[idx] = inv;
              break;
            }
            else if (Map.class == parameterType
                    || HashMap.class == parameterType) { // Map
              args[idx] = ((AttributeAccessor) inv).getAttributes();
              break;
            }
            else {
              throw new UnsupportedOperationException("Not supported " + parameterType);
            }
          }
          throw new UnsupportedOperationException("Not supported " + inv);
        }
        case TYPE_ARGUMENTS -> args[idx] = inv.getArguments();
        case TYPE_RETURNING -> args[idx] = returnValue;
        case TYPE_ANNOTATED -> args[idx] = resolveAnnotation(inv, adviceParameterTypes[idx]);
        case TYPE_JOIN_POINT -> args[idx] = inv;
        default -> {
          Class<?> parameterType = adviceParameterTypes[idx];
          if (Joinpoint.class.isAssignableFrom(parameterType)) {
            args[idx] = inv;
            break;
          }
          else if (Annotation.class.isAssignableFrom(parameterType)) {
            args[idx] = resolveAnnotation(inv, parameterType);
            break;
          }
          else if (throwable != null) {
            throwable = ExceptionUtils.unwrapThrowable(throwable);
            if (parameterType == Throwable.class //
                    || parameterType.isAssignableFrom(throwable.getClass())) //
            {
              args[idx] = throwable;
              break;
            }
          }
          else if (returnValue != null && parameterType.isAssignableFrom(returnValue.getClass())) {
            args[idx] = returnValue;
            break;
          }
          else if (inv instanceof AttributeAccessor) {
            if (AttributeAccessor.class == parameterType
                    || MethodInvocation.class == parameterType) {
              args[idx] = inv;
              break;
            }
            else if (Map.class == parameterType
                    || HashMap.class == parameterType) { // Map
              args[idx] = ((AttributeAccessor) inv).getAttributes();
              break;
            }
          }
          throw new UnsupportedOperationException("Not supported " + parameterType);
        }
      }
      idx++;
    }
    return doInvokeAdviceMethod(args);
  }

  /**
   * @param args method arguments
   * @return invoke result
   * @since 4.0
   */
  private Object doInvokeAdviceMethod(Object[] args) {
    MethodInvoker methodInvoker = this.methodInvoker;
    if (methodInvoker == null) {
      synchronized(this) {
        methodInvoker = this.methodInvoker;
        if (methodInvoker == null) {
          methodInvoker = MethodInvoker.fromMethod(adviceMethod);
          this.methodInvoker = methodInvoker;
        }
      }
    }
    return methodInvoker.invoke(beanSupplier.get(), args);
  }

  /**
   * Resolve an annotation
   *
   * @param methodInvocation The join point
   * @param annotationClass Given annotation class
   * @return Annotation
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Object resolveAnnotation(MethodInvocation methodInvocation, Class annotationClass) {
    Method method = methodInvocation.getMethod();
    Annotation annotation = AnnotationUtils.getAnnotation(method, annotationClass);
    if (annotation == null) {
      annotation = AnnotationUtils.getAnnotation(method.getDeclaringClass(), annotationClass);
    }
    return annotation;
  }

}
