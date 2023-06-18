/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.io.Closeable;
import java.io.Serial;

import cn.taketoday.aop.AopInfrastructureBean;
import cn.taketoday.aop.framework.autoproxy.AbstractAutoProxyCreator;
import cn.taketoday.beans.factory.Aware;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Base class with common functionality for proxy processors, in particular
 * ClassLoader management and the {@link #evaluateProxyInterfaces} algorithm.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/28 17:10</a>
 * @see AbstractAutoProxyCreator
 * @since 4.0
 */
public class ProxyProcessorSupport extends ProxyConfig implements Ordered, BeanClassLoaderAware, AopInfrastructureBean {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * This should run after all other processors, so that it can just add
   * an advisor to existing proxies rather than double-proxy.
   */
  private int order = Ordered.LOWEST_PRECEDENCE;

  @Nullable
  private ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

  private boolean classLoaderConfigured = false;

  /**
   * Set the ordering which will apply to this processor's implementation
   * of {@link Ordered}, used when applying multiple processors.
   * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
   *
   * @param order the ordering value
   */
  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  /**
   * Set the ClassLoader to generate the proxy class in.
   * <p>Default is the bean ClassLoader, i.e. the ClassLoader used by the containing
   * {@link BeanFactory} for loading all bean classes.This can be overridden here for specific proxies.
   */
  public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
    this.proxyClassLoader = classLoader;
    this.classLoaderConfigured = (classLoader != null);
  }

  /**
   * Return the configured proxy ClassLoader for this processor.
   */
  @Nullable
  protected ClassLoader getProxyClassLoader() {
    return this.proxyClassLoader;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    if (!this.classLoaderConfigured) {
      this.proxyClassLoader = classLoader;
    }
  }

  /**
   * Check the interfaces on the given bean class and apply them to the {@link ProxyFactory},
   * if appropriate.
   * <p>Calls {@link #isConfigurationCallbackInterface} and {@link #isInternalLanguageInterface}
   * to filter for reasonable proxy interfaces, falling back to a target-class proxy otherwise.
   *
   * @param beanClass the class of the bean
   * @param proxyFactory the ProxyFactory for the bean
   */
  protected void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
    Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, getProxyClassLoader());
    boolean hasReasonableProxyInterface = false;
    for (Class<?> ifc : targetInterfaces) {
      if (!isConfigurationCallbackInterface(ifc)
              && !isInternalLanguageInterface(ifc) && ifc.getMethods().length > 0) {
        hasReasonableProxyInterface = true;
        break;
      }
    }
    if (hasReasonableProxyInterface) {
      // Must allow for introductions; can't just set interfaces to the target's interfaces only.
      for (Class<?> ifc : targetInterfaces) {
        proxyFactory.addInterface(ifc);
      }
    }
    else {
      proxyFactory.setProxyTargetClass(true);
    }
  }

  /**
   * Determine whether the given interface is just a container callback and
   * therefore not to be considered as a reasonable proxy interface.
   * <p>If no reasonable proxy interface is found for a given bean, it will get
   * proxied with its full target class, assuming that as the user's intention.
   *
   * @param ifc the interface to check
   * @return whether the given interface is just a container callback
   */
  protected boolean isConfigurationCallbackInterface(Class<?> ifc) {
    return InitializingBean.class == ifc
            || Closeable.class == ifc
            || AutoCloseable.class == ifc
            || DisposableBean.class == ifc
            || ObjectUtils.containsElement(ifc.getInterfaces(), Aware.class);
  }

  /**
   * Determine whether the given interface is a well-known internal language interface
   * and therefore not to be considered as a reasonable proxy interface.
   * <p>If no reasonable proxy interface is found for a given bean, it will get
   * proxied with its full target class, assuming that as the user's intention.
   *
   * @param ifc the interface to check
   * @return whether the given interface is an internal language interface
   */
  protected boolean isInternalLanguageInterface(Class<?> ifc) {
    return ifc.getName().endsWith(".bytecode.proxy.Factory")
            || ifc.getName().endsWith(".bytebuddy.MockAccess");
  }

}
