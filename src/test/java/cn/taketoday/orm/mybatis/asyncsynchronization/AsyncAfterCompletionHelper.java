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
package cn.taketoday.orm.mybatis.asyncsynchronization;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.transaction.support.TransactionSynchronization;

/**
 * For use as ByteMan helper
 *
 * @author Alex Rykov
 */
@SuppressWarnings("unused")
public class AsyncAfterCompletionHelper {
  /**
   * Invocation handler that performs afterCompletion on a separate thread See Github issue #18
   *
   * @author Alex Rykov
   */
  static class AsyncAfterCompletionInvocationHandler implements InvocationHandler {

    private final Object target;

    AsyncAfterCompletionInvocationHandler(Object target) {
      this.target = target;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      if ("afterCompletion".equals(method.getName())) {
        final Set<Object> retValSet = new HashSet<>();
        final Set<Throwable> exceptionSet = new HashSet<>();
        Thread thread = new Thread(() -> {
          try {
            retValSet.add(method.invoke(target, args));
          }
          catch (InvocationTargetException ite) {
            exceptionSet.add(ite.getCause());

          }
          catch (IllegalArgumentException | IllegalAccessException e) {
            exceptionSet.add(e);

          }
        });
        thread.start();
        thread.join();
        if (exceptionSet.isEmpty()) {
          return retValSet.iterator().next();
        }
        else {
          throw exceptionSet.iterator().next();
        }
      }
      else {
        return method.invoke(target, args);
      }
    }

  }

  /**
   * Creates proxy that performs afterCompletion call on a separate thread
   */
  public TransactionSynchronization createSynchronizationWithAsyncAfterComplete(
          TransactionSynchronization synchronization) {
    if (Proxy.isProxyClass(synchronization.getClass())
            && Proxy.getInvocationHandler(synchronization) instanceof AsyncAfterCompletionInvocationHandler) {
      // avoiding double wrapping just in case
      return synchronization;
    }
    Class<?>[] interfaces = { TransactionSynchronization.class };
    return (TransactionSynchronization) Proxy.newProxyInstance(synchronization.getClass().getClassLoader(), interfaces,
            new AsyncAfterCompletionInvocationHandler(synchronization));

  }

}
