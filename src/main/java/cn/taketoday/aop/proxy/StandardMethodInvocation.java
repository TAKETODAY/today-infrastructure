/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.aop.proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import cn.taketoday.aop.support.RuntimeMethodInterceptor;
import cn.taketoday.context.reflect.MethodInvoker;

/**
 * @author TODAY <br>
 * 2018-11-10 13:14
 */
public class StandardMethodInvocation
        extends RuntimeMethodInvocation implements MethodInvocation {

  private final Object bean;
  private final Target target;
  private final Object[] args;

  /**
   * a flag show that current index of advice
   */
  private int currentAdviceIndex = 0;

  public StandardMethodInvocation(Object bean, Target target, Object[] arguments) {
    this.bean = bean;
    this.target = target;
    this.args = arguments;
  }

  @Override
  public Method getMethod() {
    return target.getMethod();
  }

  @Override
  public Object[] getArguments() {
    return args;
  }

  @Override
  protected boolean matchesRuntime(RuntimeMethodInterceptor runtimeInterceptor) {
    return runtimeInterceptor.matches(getMethod(), target.getClass(), args);
  }

  @Override
  protected Object invokeJoinPoint() {
    return target.proceed(bean, args);
  }

  @Override
  protected boolean shouldCallJoinPoint() {
    return currentAdviceIndex == target.adviceLength;
  }

  @Override
  protected MethodInterceptor currentMethodInterceptor() {
    return target.currentAdvice(currentAdviceIndex++);
  }

  @Override
  public Object getThis() {
    return bean;
  }

  @Override
  public AccessibleObject getStaticPart() {
    return target.getMethod();
  }

  @Override
  public String toString() {
    return target.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof StandardMethodInvocation)) return false;
    if (!super.equals(o)) return false;
    final StandardMethodInvocation that = (StandardMethodInvocation) o;
    return currentAdviceIndex == that.currentAdviceIndex
            && Objects.equals(target, that.target)
            && Arrays.equals(args, that.args);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(super.hashCode(), target, currentAdviceIndex);
    result = 31 * result + Arrays.hashCode(args);
    return result;
  }

  public static class Target {

    private final Method method;
    private final int adviceLength;
    private final MethodInvoker invoker;
    private final MethodInterceptor[] advices;

    public Target(Method method, MethodInterceptor[] advices) {
      this.method = method;
      this.advices = advices;
      this.adviceLength = advices.length;
      this.invoker = MethodInvoker.create(method);
    }

    public Method getMethod() {
      return method;
    }

    public final Object proceed(Object bean, Object[] args) {
      return invoker.invoke(bean, args);
    }

    public final Object invokeAdvice(final MethodInvocation invocation, final int index) throws Throwable {
      return advices[index].invoke(invocation);
    }

    public final MethodInterceptor currentAdvice(final int index) {
      return advices[index];
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Target)) return false;
      final Target target = (Target) o;
      return adviceLength == target.adviceLength
              && Objects.equals(method, target.method)
              && Objects.equals(invoker, target.invoker)
              && Arrays.equals(advices, target.advices);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(method, adviceLength, invoker);
      result = 31 * result + Arrays.hashCode(advices);
      return result;
    }

    @Override
    public String toString() {
      return "Target{" +
              "method=" + method +
              ", adviceLength=" + adviceLength +
              ", advices=" + Arrays.toString(advices) +
              '}';
    }
  }

}
