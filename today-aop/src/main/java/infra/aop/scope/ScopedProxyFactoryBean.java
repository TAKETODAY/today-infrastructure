/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aop.scope;

import java.lang.reflect.Modifier;

import infra.aop.AopInfrastructureBean;
import infra.aop.framework.ProxyConfig;
import infra.aop.framework.ProxyFactory;
import infra.aop.support.DelegatingIntroductionInterceptor;
import infra.aop.target.SimpleBeanTargetSource;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.FactoryBeanNotInitializedException;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;

/**
 * Convenient proxy factory bean for scoped objects.
 *
 * <p>Proxies created using this factory bean are thread-safe singletons
 * and may be injected into shared objects, with transparent scoping behavior.
 *
 * <p>Proxies returned by this class implement the {@link ScopedObject} interface.
 * This presently allows for removing the corresponding object from the scope,
 * seamlessly creating a new instance in the scope on next access.
 *
 * <p>Please note that the proxies created by this factory are
 * <i>class-based</i> proxies by default. This can be customized
 * through switching the "proxyTargetClass" property to "false".
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see #setProxyTargetClass
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ScopedProxyFactoryBean extends ProxyConfig implements FactoryBean<Object>, BeanFactoryAware, AopInfrastructureBean {

  /** The TargetSource that manages scoping. */
  private final SimpleBeanTargetSource scopedTargetSource = new SimpleBeanTargetSource();

  /** The name of the target bean. */
  @Nullable
  private String targetBeanName;

  /** The cached singleton proxy. */
  @Nullable
  private Object proxy;

  /**
   * Create a new ScopedProxyFactoryBean instance.
   */
  public ScopedProxyFactoryBean() {
    setProxyTargetClass(true);
  }

  /**
   * Set the name of the bean that is to be scoped.
   */
  public void setTargetBeanName(String targetBeanName) {
    this.targetBeanName = targetBeanName;
    this.scopedTargetSource.setTargetBeanName(targetBeanName);
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (!(beanFactory instanceof ConfigurableBeanFactory cbf)) {
      throw new IllegalStateException("Not running in a ConfigurableBeanFactory: " + beanFactory);
    }
    this.scopedTargetSource.setBeanFactory(beanFactory);

    ProxyFactory pf = new ProxyFactory();
    pf.copyFrom(this);
    pf.setTargetSource(this.scopedTargetSource);

    Assert.notNull(this.targetBeanName, "Property 'targetBeanName' is required");
    Class<?> beanType = beanFactory.getType(this.targetBeanName);
    if (beanType == null) {
      throw new IllegalStateException("Cannot create scoped proxy for bean '" + this.targetBeanName +
              "': Target type could not be determined at the time of proxy creation.");
    }
    if (!isProxyTargetClass() || beanType.isInterface() || Modifier.isPrivate(beanType.getModifiers())) {
      pf.setInterfaces(ClassUtils.getAllInterfacesForClass(beanType, cbf.getBeanClassLoader()));
    }

    // Add an introduction that implements only the methods on ScopedObject.
    ScopedObject scopedObject = new DefaultScopedObject(cbf, this.scopedTargetSource.getTargetBeanName());
    pf.addAdvice(new DelegatingIntroductionInterceptor(scopedObject));

    // Add the AopInfrastructureBean marker to indicate that the scoped proxy
    // itself is not subject to auto-proxying! Only its target bean is.
    pf.addInterface(AopInfrastructureBean.class);

    this.proxy = pf.getProxy(cbf.getBeanClassLoader());
  }

  @Override
  public Object getObject() {
    if (this.proxy == null) {
      throw new FactoryBeanNotInitializedException();
    }
    return this.proxy;
  }

  @Override
  public Class<?> getObjectType() {
    if (this.proxy != null) {
      return this.proxy.getClass();
    }
    return this.scopedTargetSource.getTargetClass();
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
