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

import cn.taketoday.context.AttributeAccessorSupport;
import cn.taketoday.context.reflect.MethodInvoker;

/**
 * @author TODAY <br>
 * 2018-11-10 13:14
 */
public class StandardMethodInvocation
        extends AttributeAccessorSupport implements MethodInvocation {

  private final Object[] args;
  private final Target target;

  /**
   * a flag show that current index of advice
   */
  private int currentAdviceIndex = 0;

  public StandardMethodInvocation(Target target, Object[] arguments) {
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
  public Object proceed() throws Throwable {
    final Target target = this.target;
    if (currentAdviceIndex == target.adviceLength) {
      return target.proceed(args);
    }
    return target.invokeAdvice(this, currentAdviceIndex++);
  }

  @Override
  public Object getThis() {
    return target.getThis();
  }

  @Override
  public AccessibleObject getStaticPart() {
    return target.getStaticPart();
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
            && Arrays.equals(args, that.args)
            && Objects.equals(target, that.target);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(super.hashCode(), target, currentAdviceIndex);
    result = 31 * result + Arrays.hashCode(args);
    return result;
  }

  public static class Target implements MethodInvocation {

    private final Object bean;
    private final Method method;
    private final int adviceLength;
    private final MethodInvoker invoker;
    private final MethodInterceptor[] advices;

    public Target(Object bean,
                  Method method,
                  MethodInterceptor[] advices) {
      this.bean = bean;
      this.method = method;
      this.advices = advices;
      this.adviceLength = advices.length;
      this.invoker = MethodInvoker.create(method);
    }

    @Override
    public Method getMethod() {
      return method;
    }

    @Override
    public Object[] getArguments() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object proceed() {
      return invoker.invoke(bean, advices);
    }

    public final Object proceed(Object[] args) {
      return invoker.invoke(bean, args);
    }

    public Object invokeAdvice(final MethodInvocation invocation, final int index) throws Throwable {
      return advices[index].invoke(invocation);
    }

    @Override
    public Object getThis() {
      return bean;
    }

    @Override
    public AccessibleObject getStaticPart() {
      return method;
    }

    @Override
    public String toString() {
      return new StringBuilder()//
              .append("{\n\t\"target\":\"").append(bean)//
              .append("\",\n\t\"method\":\"").append(method)//
              .append("\",\n\t\"advices\":\"").append(Arrays.toString(advices))//
              .append("\"\n}")//
              .toString();
    }

  }

}
