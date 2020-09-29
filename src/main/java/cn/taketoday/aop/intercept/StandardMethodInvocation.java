/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.aop.intercept;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Arrays;

import cn.taketoday.context.reflect.MethodInvoker;

/**
 * @author TODAY <br>
 * 2018-11-10 13:14
 */
public class StandardMethodInvocation implements MethodInvocation {

  private final Object[] args;
  private final TargetMethodInvocation target;

  /**
   * a flag show that current index of advice
   */
  private int currentAdviceIndex = 0;

  public StandardMethodInvocation(TargetMethodInvocation target, Object[] arguments) {
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
    final TargetMethodInvocation target = this.target;
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

  public static class TargetMethodInvocation implements MethodInvocation {

    private final Object target;
    private final Method method;
    private final int adviceLength;
    private final MethodInvoker invoker;
    private final MethodInterceptor[] advices;

    public TargetMethodInvocation(Object target, //@off
                                      Method method, 
                                      MethodInterceptor[] advices) {
            
            this.target = target;
            this.method = method;
            this.advices = advices;
            this.adviceLength = advices.length;
            this.invoker = MethodInvoker.create(method);
        } //@on

    @Override
    public Method getMethod() {
      return method;
    }

    @Override
    public Object[] getArguments() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object proceed() throws Throwable {
      return invoker.invoke(target, advices);
    }

    public final Object proceed(Object[] args) throws Throwable {
      return invoker.invoke(target, args);
    }

    public Object invokeAdvice(final MethodInvocation invocation, final int index) throws Throwable {
      return advices[index].invoke(invocation);
    }

    @Override
    public Object getThis() {
      return target;
    }

    @Override
    public AccessibleObject getStaticPart() {
      return method;
    }

    @Override
    public String toString() {
      return new StringBuilder()//
              .append("{\n\t\"target\":\"").append(target)//
              .append("\",\n\t\"method\":\"").append(method)//
              .append("\",\n\t\"advices\":\"").append(Arrays.toString(advices))//
              .append("\"\n}")//
              .toString();
    }

  }

}
