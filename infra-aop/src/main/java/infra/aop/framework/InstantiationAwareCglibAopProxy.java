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

import infra.bytecode.proxy.Callback;
import infra.bytecode.proxy.Enhancer;
import infra.bytecode.proxy.Factory;

/**
 * CGLIB-based {@link AopProxy} implementation that supports instantiation-aware proxy creation.
 * <p>This proxy factory creates CGLIB subclasses and allows for constructor argument injection,
 * enabling the creation of proxies for classes that do not have a default no-arg constructor.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AopProxyUtils#instantiateProxyClass(Class)
 * @since 4.0 2022/1/12 14:03
 */
class InstantiationAwareCglibAopProxy extends CglibAopProxy {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Create a new InstantiationAwareCglibAopProxy for the given AOP configuration.
   *
   * @param config the AOP configuration as AdvisedSupport object
   * @throws AopConfigException if the config is invalid. We try to throw an informative
   * exception in this case, rather than let a mysterious failure
   * happen later.
   */
  public InstantiationAwareCglibAopProxy(AdvisedSupport config) {
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
      Class<?> proxyClass = enhancer.createClass();
      Object proxy = AopProxyUtils.instantiateProxyClass(proxyClass);
      if (proxy instanceof Factory) {
        ((Factory) proxy).setCallbacks(callbacks);
      }
      return proxy;
    }
  }

}
