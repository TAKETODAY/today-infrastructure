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

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author TODAY 2018-11-10 13:14
 * @see TargetInvocation
 */
public class StandardMethodInvocation
        extends AbstractMethodInvocation implements MethodInvocation {

  private final Object proxy;
  protected Object[] args;
  protected final Object bean;
  protected final TargetInvocation target;

  /**
   * a flag show that current index of advice
   */
  protected int currentAdviceIndex = 0;

  public StandardMethodInvocation(Object proxy, Object bean, TargetInvocation target, Object[] arguments) {
    this.proxy = proxy;
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
  protected void setArguments(Object[] arguments) {
    args = arguments;
  }

  @Override
  public Class<?> getTargetClass() {
    return target.getTargetClass();
  }

  @Override
  public Object getProxy() {
    return proxy;
  }

  @Override
  protected Object invokeJoinPoint() {
    return target.proceed(bean, args);
  }

  @Override
  protected boolean hasInterceptor() {
    return currentAdviceIndex < target.getAdviceLength();
  }

  @Override
  protected Object executeInterceptor() throws Throwable {
    return target.invokeAdvice(this, currentAdviceIndex++);
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
    if (this == o)
      return true;
    if (!(o instanceof StandardMethodInvocation))
      return false;
    if (!super.equals(o))
      return false;
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

}
