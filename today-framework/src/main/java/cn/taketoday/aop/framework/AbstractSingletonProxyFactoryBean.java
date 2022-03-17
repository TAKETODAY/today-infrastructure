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

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.framework.adapter.AdvisorAdapterRegistry;
import cn.taketoday.aop.framework.adapter.DefaultAdvisorAdapterRegistry;
import cn.taketoday.aop.interceptor.PerformanceMonitorInterceptor;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.FactoryBeanNotInitializedException;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Convenient superclass for {@link FactoryBean} types that produce singleton-scoped
 * proxy objects.
 *
 * <p>Manages pre- and post-interceptors (references, rather than
 * interceptor names, as in {@link ProxyFactoryBean}) and provides
 * consistent interface management.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class AbstractSingletonProxyFactoryBean extends ProxyConfig
        implements FactoryBean<Object>, BeanClassLoaderAware, InitializingBean {

  @Nullable
  private Object target;

  @Nullable
  private Class<?>[] proxyInterfaces;

  @Nullable
  private Object[] preInterceptors;

  @Nullable
  private Object[] postInterceptors;

  /** Default is global AdvisorAdapterRegistry. */
  private AdvisorAdapterRegistry advisorAdapterRegistry = DefaultAdvisorAdapterRegistry.getInstance();

  @Nullable
  private transient ClassLoader proxyClassLoader;

  @Nullable
  private Object proxy;

  /**
   * Set the target object, that is, the bean to be wrapped with a transactional proxy.
   * <p>The target may be any object, in which case a SingletonTargetSource will
   * be created. If it is a TargetSource, no wrapper TargetSource is created:
   * This enables the use of a pooling or prototype TargetSource etc.
   *
   * @see TargetSource
   * @see SingletonTargetSource
   * @see cn.taketoday.aop.target.LazyInitTargetSource
   * @see cn.taketoday.aop.target.PrototypeTargetSource
   */
  public void setTarget(Object target) {
    this.target = target;
  }

  /**
   * Specify the set of interfaces being proxied.
   * <p>If not specified (the default), the AOP infrastructure works
   * out which interfaces need proxying by analyzing the target,
   * proxying all the interfaces that the target object implements.
   */
  public void setProxyInterfaces(Class<?>[] proxyInterfaces) {
    this.proxyInterfaces = proxyInterfaces;
  }

  /**
   * Set additional interceptors (or advisors) to be applied before the
   * implicit transaction interceptor, e.g. a PerformanceMonitorInterceptor.
   * <p>You may specify any AOP Alliance MethodInterceptors or other
   * Framework AOP Advices, as well as Framework AOP Advisors.
   *
   * @see PerformanceMonitorInterceptor
   */
  public void setPreInterceptors(Object[] preInterceptors) {
    this.preInterceptors = preInterceptors;
  }

  /**
   * Set additional interceptors (or advisors) to be applied after the
   * implicit transaction interceptor.
   * <p>You may specify any AOP Alliance MethodInterceptors or other
   * Framework AOP Advices, as well as Framework AOP Advisors.
   */
  public void setPostInterceptors(Object[] postInterceptors) {
    this.postInterceptors = postInterceptors;
  }

  /**
   * Specify the AdvisorAdapterRegistry to use.
   * Default is the global AdvisorAdapterRegistry.
   */
  public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
    this.advisorAdapterRegistry = advisorAdapterRegistry;
  }

  /**
   * Set the ClassLoader to generate the proxy class in.
   * <p>Default is the bean ClassLoader, i.e. the ClassLoader used by the
   * containing BeanFactory for loading all bean classes. This can be
   * overridden here for specific proxies.
   */
  public void setProxyClassLoader(ClassLoader classLoader) {
    this.proxyClassLoader = classLoader;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    if (this.proxyClassLoader == null) {
      this.proxyClassLoader = classLoader;
    }
  }

  @Override
  public void afterPropertiesSet() {
    if (this.target == null) {
      throw new IllegalArgumentException("Property 'target' is required");
    }
    if (this.target instanceof String) {
      throw new IllegalArgumentException("'target' needs to be a bean reference, not a bean name as value");
    }
    if (this.proxyClassLoader == null) {
      this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
    }

    ProxyFactory proxyFactory = new ProxyFactory();

    if (this.preInterceptors != null) {
      for (Object interceptor : this.preInterceptors) {
        proxyFactory.addAdvisor(this.advisorAdapterRegistry.wrap(interceptor));
      }
    }

    // Add the main interceptor (typically an Advisor).
    proxyFactory.addAdvisor(this.advisorAdapterRegistry.wrap(createMainInterceptor()));

    if (this.postInterceptors != null) {
      for (Object interceptor : this.postInterceptors) {
        proxyFactory.addAdvisor(this.advisorAdapterRegistry.wrap(interceptor));
      }
    }

    proxyFactory.copyFrom(this);

    TargetSource targetSource = createTargetSource(this.target);
    proxyFactory.setTargetSource(targetSource);

    if (this.proxyInterfaces != null) {
      proxyFactory.setInterfaces(this.proxyInterfaces);
    }
    else if (!isProxyTargetClass()) {
      // Rely on AOP infrastructure to tell us what interfaces to proxy.
      Class<?> targetClass = targetSource.getTargetClass();
      if (targetClass != null) {
        proxyFactory.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
      }
    }

    postProcessProxyFactory(proxyFactory);

    this.proxy = proxyFactory.getProxy(this.proxyClassLoader);
  }

  /**
   * Determine a TargetSource for the given target (or TargetSource).
   *
   * @param target the target. If this is an implementation of TargetSource it is
   * used as our TargetSource; otherwise it is wrapped in a SingletonTargetSource.
   * @return a TargetSource for this object
   */
  protected TargetSource createTargetSource(Object target) {
    if (target instanceof TargetSource) {
      return (TargetSource) target;
    }
    else {
      return new SingletonTargetSource(target);
    }
  }

  /**
   * A hook for subclasses to post-process the {@link ProxyFactory}
   * before creating the proxy instance with it.
   *
   * @param proxyFactory the AOP ProxyFactory about to be used
   */
  protected void postProcessProxyFactory(ProxyFactory proxyFactory) { }

  @Override
  public Object getObject() {
    if (this.proxy == null) {
      throw new FactoryBeanNotInitializedException();
    }
    return this.proxy;
  }

  @Override
  @Nullable
  public Class<?> getObjectType() {
    if (this.proxy != null) {
      return this.proxy.getClass();
    }
    if (this.proxyInterfaces != null && this.proxyInterfaces.length == 1) {
      return this.proxyInterfaces[0];
    }
    if (this.target instanceof TargetSource) {
      return ((TargetSource) this.target).getTargetClass();
    }
    if (this.target != null) {
      return this.target.getClass();
    }
    return null;
  }

  @Override
  public final boolean isSingleton() {
    return true;
  }

  /**
   * Create the "main" interceptor for this proxy factory bean.
   * Typically an Advisor, but can also be any type of Advice.
   * <p>Pre-interceptors will be applied before, post-interceptors
   * will be applied after this interceptor.
   */
  protected abstract Object createMainInterceptor();

}
