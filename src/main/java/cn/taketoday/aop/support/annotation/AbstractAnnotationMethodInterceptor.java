/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.core.Assert;
import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Constant;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.util.AnnotationUtils;
import cn.taketoday.util.ExceptionUtils;

/**
 * @author TODAY 2018-11-10 11:26
 * @see Aspect
 */
public abstract class AbstractAnnotationMethodInterceptor implements Advice, MethodInterceptor, Ordered {

  //private final Method adviceMethod;
  private final MethodInvoker invoker;
  private final byte[] adviceParameters;
  private final int adviceParameterLength;
  private final Class<?>[] adviceParameterTypes;

//  final BeanFactory beanFactory;
//  final BeanDefinition aspectDef;
  final ObjectSupplier<Object> aspectSupplier;

  public AbstractAnnotationMethodInterceptor(
          Method adviceMethod, BeanFactory beanFactory, BeanDefinition aspectDef) {
    Assert.notNull(beanFactory, "beanFactory must not be null");
    Assert.notNull(adviceMethod, "adviceMethod must not be null");
    Assert.notNull(aspectDef, "aspect bean definition must not be null");

    this.aspectSupplier = beanFactory.getBeanSupplier(aspectDef);

    this.invoker = MethodInvoker.create(adviceMethod, aspectDef.getBeanClass());
    this.adviceParameterLength = adviceMethod.getParameterCount();
    this.adviceParameters = new byte[adviceParameterLength];
    this.adviceParameterTypes = adviceMethod.getParameterTypes();

    Parameter[] parameters = adviceMethod.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      adviceParameters[i] = Constant.TYPE_NULL;
      if (parameter.isAnnotationPresent(JoinPoint.class)) {
        adviceParameters[i] = Constant.TYPE_JOIN_POINT;
      }
      if (parameter.isAnnotationPresent(Argument.class)) {
        adviceParameters[i] = Constant.TYPE_ARGUMENT;
      }
      if (parameter.isAnnotationPresent(Arguments.class)) {
        adviceParameters[i] = Constant.TYPE_ARGUMENTS;
      }
      if (parameter.isAnnotationPresent(Returning.class)) {
        adviceParameters[i] = Constant.TYPE_RETURNING;
      }
      if (parameter.isAnnotationPresent(Throwing.class)) {
        adviceParameters[i] = Constant.TYPE_THROWING;
      }
      if (parameter.isAnnotationPresent(Annotated.class)) {
        adviceParameters[i] = Constant.TYPE_ANNOTATED;
      }
      if (parameter.isAnnotationPresent(Attribute.class)) {
        adviceParameters[i] = Constant.TYPE_ATTRIBUTE;
      }
    }
  }

  public abstract Object invoke(MethodInvocation invocation) throws Throwable;

  /**
   * Invoke advice method
   *
   * @param inv
   *         Target method invocation
   * @param returnValue
   *         Target method return value
   * @param throwable
   *         Target method throws {@link Exception}
   */
  protected Object invokeAdviceMethod(
          final MethodInvocation inv, final Object returnValue, Throwable throwable
  ) {
    if (adviceParameterLength == 0) {
      return invoker.invoke(obtainAspectInstance(), null);
    }

    int idx = 0;
    final Object[] args = new Object[adviceParameterLength];
    final Class<?>[] adviceParameterTypes = this.adviceParameterTypes;
    for (final byte adviceParameter : this.adviceParameters) {
      switch (adviceParameter) {
        case Constant.TYPE_THROWING: {
          if (throwable != null) {
            final Class<?> parameterType = adviceParameterTypes[idx];
            throwable = ExceptionUtils.unwrapThrowable(throwable);
            if (parameterType == Throwable.class //
                    || parameterType.isAssignableFrom(throwable.getClass())) //
            {
              args[idx] = throwable;
            }
          }
          break;
        }
        case Constant.TYPE_ARGUMENT: {
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
          break;
        }
        case Constant.TYPE_ATTRIBUTE:
          if (inv instanceof AttributeAccessor) {
            final Class<?> parameterType = adviceParameterTypes[idx];
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
              throw new ConfigurationException("Not supported " + parameterType);
            }
          }
          throw new ConfigurationException("Not supported " + inv);
        case Constant.TYPE_ARGUMENTS:
          args[idx] = inv.getArguments();
          break;
        case Constant.TYPE_RETURNING:
          args[idx] = returnValue;
          break;
        case Constant.TYPE_ANNOTATED:
          args[idx] = resolveAnnotation(inv, adviceParameterTypes[idx]);
          break;
        case Constant.TYPE_JOIN_POINT:
          args[idx] = inv;
          break;
        default: {
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
          throw new ConfigurationException("Not supported " + parameterType);
        }
      }
      idx++;
    }
    return invoker.invoke(obtainAspectInstance(), args);
  }

  /**
   * Resolve an annotation
   *
   * @param methodInvocation
   *         The join point
   * @param annotationClass
   *         Given annotation class
   *
   * @return Annotation
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Object resolveAnnotation(MethodInvocation methodInvocation, Class annotationClass) {
    final Method method = methodInvocation.getMethod();
    Annotation annotation = AnnotationUtils.getAnnotation(annotationClass, method);
    if (annotation == null) {
      annotation = AnnotationUtils.getAnnotation(annotationClass, method.getDeclaringClass());
    }
    return annotation;
  }

  private Object obtainAspectInstance() {
    return aspectSupplier.get();
  }

}
