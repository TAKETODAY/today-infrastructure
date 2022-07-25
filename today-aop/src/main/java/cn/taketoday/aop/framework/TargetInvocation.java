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

package cn.taketoday.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.aop.TargetClassAware;
import cn.taketoday.reflect.MethodInvoker;

/**
 * @author TODAY 2021/3/7 21:55
 * @since 3.0
 */
public class TargetInvocation implements TargetClassAware {

  private static final Map<String, TargetInvocation> targetMap = new HashMap<>();

  private final Method method;
  private final Class<?> targetClass;
  private final AdvisedSupport config;
  private final MethodInvoker invoker;

  private int adviceLength;
  private MethodInterceptor[] interceptors;

  /**
   * @since 4.0
   */
  public TargetInvocation(Method method, AdvisedSupport config) {
    this(method, method.getDeclaringClass(), config);
  }

  public TargetInvocation(Method method, Class<?> targetClass, AdvisedSupport config) {
    this.method = method;
    this.config = config;
    this.targetClass = targetClass;
    this.invoker = MethodInvoker.fromMethod(method, targetClass);
  }

  public Method getMethod() {
    return method;
  }

  public final Object proceed(Object bean, Object[] args) {
    return invoker.invoke(bean, args);
  }

  public final Object invokeAdvice(final MethodInvocation invocation, final int index) throws Throwable {
    return currentAdvice(index).invoke(invocation);
  }

  public final MethodInterceptor currentAdvice(final int index) {
    return getInterceptors()[index];
  }

  public int getAdviceLength() {
    return adviceLength;
  }

  public MethodInvoker getInvoker() {
    return invoker;
  }

  @Override
  public Class<?> getTargetClass() {
    return targetClass;
  }

  public MethodInterceptor[] getDynamicInterceptors(AdvisedSupport config) {
    return config.getInterceptors(method, targetClass);
  }

  public MethodInterceptor[] getInterceptors() {
    MethodInterceptor[] ret = this.interceptors;
    if (ret == null) {
      ret = this.interceptors = getDynamicInterceptors(this.config);
      adviceLength = ret.length;
    }
    return ret;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final TargetInvocation target))
      return false;
    return adviceLength == target.adviceLength
            && Objects.equals(method, target.method)
            && Objects.equals(invoker, target.invoker)
            && Objects.equals(targetClass, target.targetClass)
            && Objects.equals(config, target.config);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, targetClass, config);
  }

  @Override
  public String toString() {
    return "TargetInvocation{" +
            "method=" + method +
            ", targetClass=" + targetClass +
            ", config=" + config +
            '}';
  }

  //

  public static TargetInvocation getTarget(String key) {
    return targetMap.get(key);
  }

  public static void putTarget(String key, TargetInvocation target) {
    targetMap.put(key, target);
  }
}
