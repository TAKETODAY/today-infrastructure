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

package infra.jmx.access;

import infra.aop.framework.ProxyFactory;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.InitializingBean;
import infra.jmx.MBeanServerNotFoundException;
import infra.lang.Nullable;
import infra.util.ClassUtils;

/**
 * Creates a proxy to a managed resource running either locally or remotely.
 * The "proxyInterface" property defines the interface that the generated
 * proxy is supposed to implement. This interface should define methods and
 * properties that correspond to operations and attributes in the management
 * interface of the resource you wish to proxy.
 *
 * <p>There is no need for the managed resource to implement the proxy interface,
 * although you may find it convenient to do. It is not required that every
 * operation and attribute in the management interface is matched by a
 * corresponding property or method in the proxy interface.
 *
 * <p>Attempting to invoke or access any method or property on the proxy
 * interface that does not correspond to the management interface will lead
 * to an {@code InvalidInvocationException}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see MBeanClientInterceptor
 * @see InvalidInvocationException
 * @since 4.0
 */
public class MBeanProxyFactoryBean extends MBeanClientInterceptor
        implements FactoryBean<Object>, BeanClassLoaderAware, InitializingBean {

  @Nullable
  private Class<?> proxyInterface;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  @Nullable
  private Object mbeanProxy;

  /**
   * Set the interface that the generated proxy will implement.
   * <p>This will usually be a management interface that matches the target MBean,
   * exposing bean property setters and getters for MBean attributes and
   * conventional Java methods for MBean operations.
   *
   * @see #setObjectName
   */
  public void setProxyInterface(Class<?> proxyInterface) {
    this.proxyInterface = proxyInterface;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  /**
   * Checks that the {@code proxyInterface} has been specified and then
   * generates the proxy for the target MBean.
   */
  @Override
  public void afterPropertiesSet() throws MBeanServerNotFoundException, MBeanInfoRetrievalException {
    super.afterPropertiesSet();

    if (this.proxyInterface == null) {
      this.proxyInterface = getManagementInterface();
      if (this.proxyInterface == null) {
        throw new IllegalArgumentException("Property 'proxyInterface' or 'managementInterface' is required");
      }
    }
    else {
      if (getManagementInterface() == null) {
        setManagementInterface(this.proxyInterface);
      }
    }
    this.mbeanProxy = new ProxyFactory(this.proxyInterface, this).getProxy(this.beanClassLoader);
  }

  @Override
  @Nullable
  public Object getObject() {
    return this.mbeanProxy;
  }

  @Override
  public Class<?> getObjectType() {
    return this.proxyInterface;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
