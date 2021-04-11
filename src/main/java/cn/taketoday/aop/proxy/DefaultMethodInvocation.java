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

import cn.taketoday.aop.support.AopUtils;

/**
 * @author TODAY <br>
 * 2018-11-10 13:14
 */
public class DefaultMethodInvocation extends AbstractMethodInvocation implements MethodInvocation {

  protected Object[] args;
  protected final Object target;
  protected final Method method;
  protected final Class<?> targetClass;
  protected final MethodInterceptor[] advices;

  /**
   * a flag show that current index of advice
   */
  private int currentAdviceIndex = 0;

  private final int adviceLength;

  public DefaultMethodInvocation(Method method, Class<?> targetClass, Object[] arguments) {
    this(null, method, targetClass, arguments, null);
  }

  public DefaultMethodInvocation(Object target,
                                 Method method,
                                 Class<?> targetClass,
                                 Object[] arguments,
                                 MethodInterceptor[] advices) {
    this.target = target;
    this.method = method;
    this.targetClass = targetClass;
    this.args = arguments;
    this.advices = advices;
    if (advices != null)
      this.adviceLength = advices.length;
    else {
      this.adviceLength = 0;
    }
  }

  @Override
  public Method getMethod() {
    return method;
  }

  @Override
  public Object[] getArguments() {
    return args;
  }

  @Override
  protected void setArguments(Object[] arguments) {
    args = arguments;
  }

  @Override
  protected Object invokeJoinPoint() throws Throwable {
    return AopUtils.invokeJoinpointUsingReflection(target, method, args);
  }

  @Override
  protected boolean hasInterceptor() {
    return currentAdviceIndex < adviceLength;
  }

  @Override
  protected Object executeInterceptor() throws Throwable {
    return advices[currentAdviceIndex++].invoke(this);
  }

  @Override
  public Object getThis() {
    return target;
  }

  @Override
  public Class<?> getTargetClass() {
    return targetClass;
  }

  @Override
  public AccessibleObject getStaticPart() {
    return method;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DefaultMethodInvocation)) return false;
    if (!super.equals(o)) return false;
    final DefaultMethodInvocation that = (DefaultMethodInvocation) o;
    return currentAdviceIndex == that.currentAdviceIndex
            && adviceLength == that.adviceLength
            && Arrays.equals(args, that.args)
            && Objects.equals(target, that.target)
            && Objects.equals(method, that.method)
            && Arrays.equals(advices, that.advices);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(super.hashCode(), target, method);
    result = 31 * result + Arrays.hashCode(args);
    result = 31 * result + Arrays.hashCode(advices);
    return result;
  }

  @Override
  public String toString() {
    return new StringBuilder()//
            .append("{\n\t\"target\":\"").append(target)//
            .append("\",\n\t\"method\":\"").append(method)//
            .append("\",\n\t\"arguments\":\"").append(Arrays.toString(args))//
            .append("\",\n\t\"advices\":\"").append(Arrays.toString(advices))//
            .append("\",\n\t\"currentAdviceIndex\":\"").append(currentAdviceIndex)//
            .append("\"\n}")//
            .toString();
  }

}
