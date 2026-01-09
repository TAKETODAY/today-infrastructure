/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
