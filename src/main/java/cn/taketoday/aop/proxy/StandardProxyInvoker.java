/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import java.lang.reflect.Method;

import cn.taketoday.aop.AopInvocationException;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.framework.AdvisedSupport;
import cn.taketoday.aop.framework.AopContext;
import cn.taketoday.aop.framework.StandardMethodInvocation;
import cn.taketoday.util.ObjectUtils;

/**
 * @author TODAY 2021/2/16 22:58
 * @since 3.0
 */
public abstract class StandardProxyInvoker {

  public static Object proceed(Object proxy, Object target, TargetInvocation targetInv, Object[] args) throws Throwable {
    return new StandardMethodInvocation(proxy, target, targetInv, args).proceed();
  }

  public static Object staticExposeProceed(
          Object proxy, Object target, TargetInvocation targetInv, Object[] args) throws Throwable {
    Object oldProxy = null;
    try {
      oldProxy = AopContext.setCurrentProxy(proxy);
      return proceed(proxy, target, targetInv, args);
    }
    finally {
      AopContext.setCurrentProxy(oldProxy);
    }
  }

  public static Object dynamicExposeProceed(
          Object proxy, TargetSource targetSource, TargetInvocation targetInv, Object[] args) throws Throwable {

    Object oldProxy = null;
    final Object target = targetSource.getTarget();
    try {
      oldProxy = AopContext.setCurrentProxy(proxy);
      return proceed(proxy, target, targetInv, args);
    }
    finally {
      AopContext.setCurrentProxy(oldProxy);
      if (target != null && !targetSource.isStatic()) {
        targetSource.releaseTarget(target);
      }
    }
  }

  public static Object dynamicProceed(
          Object proxy, TargetSource targetSource, TargetInvocation targetInv, Object[] args) throws Throwable {

    final Object target = targetSource.getTarget();
    try {
      return proceed(proxy, target, targetInv, args);
    }
    finally {
      if (target != null && !targetSource.isStatic()) {
        targetSource.releaseTarget(target);
      }
    }
  }

  public static Object dynamicAdvisedProceed(
          Object proxy, AdvisedSupport advised, TargetInvocation targetInv, Object[] args) throws Throwable {

    Object target = null;
    Object oldProxy = null;
    boolean restore = false;

    final TargetSource targetSource = advised.getTargetSource();
    try {
      if (advised.isExposeProxy()) {
        // Make invocation available if necessary.
        oldProxy = AopContext.setCurrentProxy(proxy);
        restore = true;
      }
      target = targetSource.getTarget();

      final MethodInterceptor[] interceptors = targetInv.getDynamicInterceptors(advised);
      // Check whether we only have one Interceptor: that is, no real advice,
      // but just use MethodInvoker invocation of the target.
      if (ObjectUtils.isEmpty(interceptors)) {
        return targetInv.proceed(target, args);
      }

      // We need to create a DynamicStandardMethodInvocation...
      final Object retVal = new DynamicStandardMethodInvocation(
              proxy, target, targetInv, args, interceptors).proceed();
      assertReturnValue(retVal, targetInv.getMethod());
      return retVal;
    }
    finally {
      if (target != null && !targetSource.isStatic()) {
        targetSource.releaseTarget(target);
      }
      if (restore) {
        // Restore old proxy.
        AopContext.setCurrentProxy(oldProxy);
      }
    }
  }

  public static void assertReturnValue(Object retVal, Method method) {
    Class<?> returnType;
    if (retVal == null && (returnType = method.getReturnType()) != Void.TYPE && returnType.isPrimitive()) {
      throw new AopInvocationException(
              "Null return value from advice does not match primitive return type for: " + method);
    }
  }

  public static Object processReturnValue(Object proxy, Object target, Object retVal, Method method) {
    // Massage return value if necessary.
    Class<?> returnType;
    if (retVal != null && retVal == target &&
            (returnType = method.getReturnType()) != Object.class && returnType.isInstance(proxy)) {
      // Special case: it returned "this" and the return type of the method
      // is type-compatible. Note that we can't help if the target sets
      // a reference to itself in another returned object.
      retVal = proxy;
    }
    if (retVal == null && (returnType = method.getReturnType()) != Void.TYPE && returnType.isPrimitive()) {
      throw new AopInvocationException(
              "Null return value from advice does not match primitive return type for: " + method);
    }
    return retVal;
  }

}

