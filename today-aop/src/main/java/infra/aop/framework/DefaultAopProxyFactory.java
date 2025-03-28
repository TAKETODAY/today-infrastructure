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
import java.io.Serializable;
import java.lang.reflect.Proxy;

import infra.util.ClassUtils;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * <ul>
 * <li>the {@code optimize} flag is set
 * <li>the {@code proxyTargetClass} flag is set
 * <li>no proxy interfaces have been specified
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 * @since 4.0
 */
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Singleton instance of this class.
   */
  public static final DefaultAopProxyFactory INSTANCE = new DefaultAopProxyFactory();

  @Override
  public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
    if (config.isOptimize() || config.isProxyTargetClass() || !config.hasUserSuppliedInterfaces()) {
      Class<?> targetClass = config.getTargetClass();
      if (targetClass == null && config.getProxiedInterfaces().length == 0) {
        throw new AopConfigException("TargetSource cannot determine target class: " +
                "Either an interface or a target is required for proxy creation.");
      }
      if (targetClass == null || targetClass.isInterface()
              || Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
        return new JdkDynamicAopProxy(config);
      }
      return new SerializationCglibAopProxy(config);
    }
    else {
      return new JdkDynamicAopProxy(config);
    }
  }

}
