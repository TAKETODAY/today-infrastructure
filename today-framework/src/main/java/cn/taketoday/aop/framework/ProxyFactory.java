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

import org.aopalliance.intercept.Interceptor;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.util.ClassUtils;

/**
 * Factory for AOP proxies for programmatic use, rather than via declarative
 * setup in a bean factory. This class provides a simple way of obtaining
 * and configuring AOP proxy instances in custom user code.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author TODAY 2021/2/1 22:58
 * @since 3.0
 */
public class ProxyFactory extends ProxyCreatorSupport {
  private static final long serialVersionUID = 1L;

  public ProxyFactory() { }

  /**
   * Create a new ProxyFactory.
   * <p>Will proxy all interfaces that the given target implements.
   *
   * @param target the target object to be proxied
   */
  public ProxyFactory(Object target) {
    setTarget(target);
    setInterfaces(ClassUtils.getAllInterfaces(target));
  }

  /**
   * Create a new ProxyFactory.
   * <p>No target, only interfaces. Must add interceptors.
   *
   * @param proxyInterfaces the interfaces that the proxy should implement
   */
  public ProxyFactory(Class<?>... proxyInterfaces) {
    setInterfaces(proxyInterfaces);
  }

  /**
   * Create a new ProxyFactory for the given interface and interceptor.
   * <p>Convenience method for creating a proxy for a single interceptor,
   * assuming that the interceptor handles all calls itself rather than
   * delegating to a target, like in the case of remoting proxies.
   *
   * @param proxyInterface the interface that the proxy should implement
   * @param interceptor the interceptor that the proxy should invoke
   */
  public ProxyFactory(Class<?> proxyInterface, Interceptor interceptor) {
    addInterface(proxyInterface);
    addAdvice(interceptor);
  }

  /**
   * Create a ProxyFactory for the specified {@code TargetSource},
   * making the proxy implement the specified interface.
   *
   * @param proxyInterface the interface that the proxy should implement
   * @param targetSource the TargetSource that the proxy should invoke
   */
  public ProxyFactory(Class<?> proxyInterface, TargetSource targetSource) {
    addInterface(proxyInterface);
    setTargetSource(targetSource);
  }

  /**
   * Create a new proxy according to the settings in this factory.
   * <p>Can be called repeatedly. Effect will vary if we've added
   * or removed interfaces. Can add and remove interceptors.
   * <p>Uses a default class loader: Usually, the thread context class loader
   * (if necessary for proxy creation).
   *
   * @return the proxy object
   */
  public Object getProxy() {
    return createAopProxy().getProxy();
  }

  /**
   * Create a new proxy according to the settings in this factory.
   * <p>Can be called repeatedly. Effect will vary if we've added
   * or removed interfaces. Can add and remove interceptors.
   * <p>Uses the given class loader (if necessary for proxy creation).
   *
   * @param classLoader the class loader to create the proxy with
   * (or {@code null} for the low-level proxy facility's default)
   * @return the proxy object
   */
  public Object getProxy(ClassLoader classLoader) {
    return createAopProxy().getProxy(classLoader);
  }

  /**
   * Create a new proxy for the given interface and interceptor.
   * <p>Convenience method for creating a proxy for a single interceptor,
   * assuming that the interceptor handles all calls itself rather than
   * delegating to a target, like in the case of remoting proxies.
   *
   * @param proxyInterface the interface that the proxy should implement
   * @param interceptor the interceptor that the proxy should invoke
   * @return the proxy object
   * @see #ProxyFactory(Class, org.aopalliance.intercept.Interceptor)
   */
  @SuppressWarnings("unchecked")
  public static <T> T getProxy(Class<T> proxyInterface, Interceptor interceptor) {
    return (T) new ProxyFactory(proxyInterface, interceptor).getProxy();
  }

  /**
   * Create a proxy for the specified {@code TargetSource},
   * implementing the specified interface.
   *
   * @param proxyInterface the interface that the proxy should implement
   * @param targetSource the TargetSource that the proxy should invoke
   * @return the proxy object
   * @see #ProxyFactory(Class, TargetSource)
   */
  @SuppressWarnings("unchecked")
  public static <T> T getProxy(Class<T> proxyInterface, TargetSource targetSource) {
    return (T) new ProxyFactory(proxyInterface, targetSource).getProxy();
  }

  /**
   * Create a proxy for the specified {@code TargetSource} that extends
   * the target class of the {@code TargetSource}.
   *
   * @param targetSource the TargetSource that the proxy should invoke
   * @return the proxy object
   */
  public static Object getProxy(TargetSource targetSource) {
    if (targetSource.getTargetClass() == null) {
      throw new IllegalArgumentException("Cannot create class proxy for TargetSource with null target class");
    }
    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.setTargetSource(targetSource);
    proxyFactory.setProxyTargetClass(true);
    return proxyFactory.getProxy();
  }

}
