/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aop.framework;

import java.io.Serial;
import java.lang.reflect.Constructor;

import infra.beans.support.BeanInstantiator;
import infra.beans.support.SunReflectionFactoryInstantiator;
import infra.bytecode.proxy.Callback;
import infra.bytecode.proxy.Enhancer;
import infra.bytecode.proxy.Factory;
import infra.util.ReflectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SunReflectionFactoryInstantiator
 * @since 4.0 2022/1/12 14:03
 */
class SerializationCglibAopProxy extends CglibAopProxy {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Create a new SerializationCglibAopProxy for the given AOP configuration.
   *
   * @param config the AOP configuration as AdvisedSupport object
   * @throws AopConfigException if the config is invalid. We try to throw an informative
   * exception in this case, rather than let a mysterious failure
   * happen later.
   */
  public SerializationCglibAopProxy(AdvisedSupport config) {
    super(config);
  }

  @Override
  protected Class<?> createProxyClass(Enhancer enhancer) {
    return enhancer.createClass();
  }

  @Override
  protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) throws Exception {
    if (constructorArgs != null && constructorArgTypes != null) {
      // use constructor
      enhancer.setCallbacks(callbacks);
      return enhancer.create(constructorArgTypes, constructorArgs);
    }
    else {
      Object proxy;
      // use default constructor
      Class<?> proxyClass = enhancer.createClass();
      Constructor<?> constructor = ReflectionUtils.getConstructorIfAvailable(proxyClass);
      if (constructor != null) {
        proxy = constructor.newInstance();
      }
      else {
        // use SunReflectionFactoryInstantiator
        proxy = BeanInstantiator.forSerialization(proxyClass).instantiate();
      }
      if (proxy instanceof Factory) {
        ((Factory) proxy).setCallbacks(callbacks);
      }
      return proxy;
    }
  }

}
