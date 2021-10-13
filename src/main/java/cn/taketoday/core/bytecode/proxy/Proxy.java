/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.core.bytecode.proxy;

import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.core.bytecode.core.CodeGenerationException;
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
    return new Enhancer()
            .setClassLoader(loader)
            .setInterfaces(interfaces)
            .setSuperclass(ProxyImpl.class)
            .setCallbackTypes(InvocationHandler.class, NoOp.class)
            .setCallbackFilter(BAD_OBJECT_METHOD_FILTER)
            .setUseFactory(false)
            .createClass();
  }

  public static boolean isProxyClass(Class<?> cl) {
    return cl.getSuperclass().equals(ProxyImpl.class);
  }

  /**
   * Returns an instance of a proxy class for the specified interfaces that
   * dispatches method invocations to the specified invocation handler.
   *
   * <p>
   * {@code Proxy.newProxyInstance} throws {@code IllegalArgumentException} for
   * the same reasons that {@code Proxy.getProxyClass} does.
   *
   * @param loader
   *         the class loader to define the proxy class
   * @param interfaces
   *         the list of interfaces for the proxy class to implement
   * @param h
   *         the invocation handler to dispatch method invocations to
   *
   * @return a proxy instance with the specified invocation handler of a proxy
   * class that is defined by the specified class loader and that
   * implements the specified interfaces
   *
   * @throws NullPointerException
   *         if the {@code interfaces} array argument or any of its elements
   *         are {@code null}, or if the invocation handler, {@code h}, is
   *         {@code null}
   */
  public static Object newProxyInstance(
          final ClassLoader loader, final Class<?>[] interfaces, final InvocationHandler h) {
    try {
      return getProxyClass(loader, interfaces)//
              .getConstructor(InvocationHandler.class)//
              .newInstance(Objects.requireNonNull(h));
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new CodeGenerationException(e);
    }
  }
}
