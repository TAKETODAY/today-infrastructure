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
package cn.taketoday.aop.annotation;

import org.aopalliance.intercept.MethodInterceptor;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Objects;

import cn.taketoday.aop.advice.AbstractAdvice;
import cn.taketoday.aop.advice.ClassMatcher;
import cn.taketoday.aop.advice.MethodMatcher;

/**
 * @author Today <br>
 *
 * 2018-11-10 13:10
 */
@SuppressWarnings("all")
public class AdviceImpl implements Advice {

  private String[] pointcut = {};
  private Class<?>[] target = null;
  private String method[] = { "*" };
  private Class<? extends Annotation>[] value;
  private Class<? extends ClassMatcher> classMatcher;
  private Class<? extends MethodMatcher> methodMatcher;
  private Class<? extends MethodInterceptor> interceptor = AbstractAdvice.class;

  @Override
  public Class<? extends Annotation> annotationType() {
    return Advice.class;
  }

  @Override
  public Class<? extends Annotation>[] value() {
    return this.value;
  }

  @Override
  public Class<?>[] target() {
    return target;
  }

  @Override
  public String[] method() {
    return this.method;
  }

  @Override
  public String[] pointcut() {
    return this.pointcut;
  }

  @Override
  public Class<? extends MethodInterceptor> interceptor() {
    return this.interceptor;
  }

  @Override
  public String toString() {
    return new StringBuilder()//
            .append("{\n\t\"value\":\"").append(Arrays.toString(value))//
            .append("\",\n\t\"method\":\"").append(Arrays.toString(method))//
            .append("\",\n\t\"pointcut\":\"").append(Arrays.toString(pointcut))//
            .append("\",\n\t\"target\":\"").append(Arrays.toString(target))//
            .append("\",\n\t\"interceptor\":\"").append(interceptor)//
            .append("\"\n}")//
            .toString();
  }

  @Override
  public Class<? extends ClassMatcher> classMatcher() {
    return classMatcher;
  }

  @Override
  public Class<? extends MethodMatcher> methodMatcher() {
    return methodMatcher;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AdviceImpl)) return false;
    final AdviceImpl advice = (AdviceImpl) o;
    return Arrays.equals(pointcut, advice.pointcut)
            && Arrays.equals(value, advice.value)
            && Arrays.equals(method, advice.method)
            && Arrays.equals(target, advice.target)
            && Objects.equals(interceptor, advice.interceptor)
            && Objects.equals(classMatcher, advice.classMatcher)
            && Objects.equals(methodMatcher, advice.methodMatcher);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(classMatcher, methodMatcher, interceptor);
    result = 31 * result + Arrays.hashCode(pointcut);
    result = 31 * result + Arrays.hashCode(target);
    result = 31 * result + Arrays.hashCode(method);
    result = 31 * result + Arrays.hashCode(value);
    return result;
  }
}
