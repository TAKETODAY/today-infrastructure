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

import cn.taketoday.aop.TargetSource;
import cn.taketoday.context.utils.ObjectUtils;

/**
 * @author TODAY 2021/2/16 22:58
 */
public interface StandardProxyInvoker {

  static Object proceed(Object target, TargetInvocation targetInv, Object[] args) throws Throwable {
    return new StandardMethodInvocation(target, targetInv, args).proceed();
  }

  static Object staticExposeProceed(Object proxy, Object target,
                                    TargetInvocation targetInv, Object[] args) throws Throwable {
    Object oldProxy = null;
    try {
      oldProxy = AopContext.setCurrentProxy(proxy);
      return proceed(target, targetInv, args);
    }
    finally {
      AopContext.setCurrentProxy(oldProxy);
    }
  }

  static Object dynamicExposeProceed(Object proxy, TargetSource targetSource,
                                     TargetInvocation targetInv, Object[] args) throws Throwable {

    Object oldProxy = null;
    final Object target = targetSource.getTarget();
    try {
      oldProxy = AopContext.setCurrentProxy(proxy);
      return proceed(target, targetInv, args);
    }
    finally {
      AopContext.setCurrentProxy(oldProxy);
      if (target != null && !targetSource.isStatic()) {
        targetSource.releaseTarget(target);
      }
    }
  }

  static Object dynamicProceed(TargetSource targetSource,
                               TargetInvocation targetInv, Object[] args) throws Throwable {

    final Object target = targetSource.getTarget();
    try {
      return proceed(target, targetInv, args);
    }
    finally {
      if (target != null && !targetSource.isStatic()) {
        targetSource.releaseTarget(target);
      }
    }
  }

  static Object dynamicAdvisedProceed(Object proxy, AdvisedSupport advised,
                                      TargetInvocation targetInv, Object[] args) throws Throwable {

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
      return new DynamicStandardMethodInvocation(target, targetInv, args, interceptors).proceed();
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

}

