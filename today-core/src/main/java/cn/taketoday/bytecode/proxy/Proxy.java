/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.bytecode.proxy;

import java.io.Serial;
import java.io.Serializable;

import cn.taketoday.bytecode.core.CodeGenerationException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;

/**
 * This class is meant to be used as replacement for
 * <code>java.lang.reflect.Proxy</code> under JDK 1.2. There are some known
 * subtle differences:
 * <ul>
 * <li>The exceptions returned by invoking <code>getExceptionTypes</code> on the
 * <code>Method</code> passed to the <code>invoke</code> method <b>are</b> the
 * exact set that can be thrown without resulting in an
 * <code>UndeclaredThrowableException</code> being thrown.
 * <li>{@link UndeclaredThrowableException} is used instead of
 * <code>java.lang.reflect.UndeclaredThrowableException</code>.
 * </ul>
 * <p>
 *
 * @author TODAY <br>
 * 2019-09-03 18:51
 */
public class Proxy implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  protected final InvocationHandler h;

  private static final CallbackFilter BAD_OBJECT_METHOD_FILTER = (method) -> {
    if ("java.lang.Object".equals(method.getDeclaringClass().getName())) {
      final String name = method.getName();
      if (!(name.equals(Constant.HASH_CODE)
              || name.equals(Constant.EQUALS)
              || name.equals(Constant.TO_STRING))) {
        return 1;
      }
    }
    return 0;
  };

  protected Proxy(InvocationHandler h) {
    this.h = h;
    Enhancer.registerCallbacks(getClass(), new Callback[] { h, null });
  }

  // private for security of isProxyClass
  private static class ProxyImpl extends Proxy {
    @Serial
    private static final long serialVersionUID = 1L;

    protected ProxyImpl(InvocationHandler h) {
      super(h);
    }
  }

  public static InvocationHandler getInvocationHandler(final Object proxy) {
    if (proxy instanceof ProxyImpl) {
      return ((Proxy) proxy).h;
    }
    throw new IllegalArgumentException("Object is not a proxy");
  }

  public static Class<?> getProxyClass(final ClassLoader loader, final Class<?>... interfaces) {
    Enhancer enhancer = new Enhancer();
    enhancer.setClassLoader(loader);
    enhancer.setUseFactory(false);
    enhancer.setInterfaces(interfaces);
    enhancer.setSuperclass(ProxyImpl.class);
    enhancer.setCallbackTypes(InvocationHandler.class, NoOp.class);
    enhancer.setCallbackFilter(BAD_OBJECT_METHOD_FILTER);
    return enhancer.createClass();
  }

  /**
   * Returns true if the given class is a proxy class.
   *
   * @param cl the class to test
   * @return {@code true} if the class is a proxy class and
   * {@code false} otherwise
   * @throws NullPointerException if {@code cl} is {@code null}
   * @implNote The reliability of this method is important for the ability
   * to use it to make security decisions, so its implementation should
   * not just test if the class in question extends {@code Proxy}.
   */
  public static boolean isProxyClass(Class<?> cl) {
    return ProxyImpl.class.isAssignableFrom(cl);
  }

  /**
   * Returns an instance of a proxy class for the specified interfaces that
   * dispatches method invocations to the specified invocation handler.
   *
   * <p>
   * {@code Proxy.newProxyInstance} throws {@code IllegalArgumentException} for
   * the same reasons that {@code Proxy.getProxyClass} does.
   *
   * @param loader the class loader to define the proxy class
   * @param interfaces the list of interfaces for the proxy class to implement
   * @param handler the invocation handler to dispatch method invocations to
   * @return a proxy instance with the specified invocation handler of a proxy
   * class that is defined by the specified class loader and that
   * implements the specified interfaces
   * @throws IllegalArgumentException if the {@code interfaces} array argument or any of its elements
   * are {@code null}, or if the invocation handler, {@code h}, is
   * {@code null}
   */
  public static Object newProxyInstance(ClassLoader loader,
          Class<?>[] interfaces, InvocationHandler handler) {
    Assert.notNull(handler, "InvocationHandler is required");
    try {
      return getProxyClass(loader, interfaces)
              .getConstructor(InvocationHandler.class)
              .newInstance(handler);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new CodeGenerationException(e);
    }
  }
}
