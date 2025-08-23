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

package infra.aop.framework;

import java.io.Serial;
import java.util.concurrent.ConcurrentHashMap;

import infra.aop.Advisor;
import infra.aop.AopInfrastructureBean;
import infra.aop.support.AopUtils;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import infra.core.SmartClassLoader;
import infra.lang.Nullable;

/**
 * Base class for {@link BeanPostProcessor} implementations that apply a
 * AOP {@link Advisor} to specific beans.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractAdvisingBeanPostProcessor extends ProxyProcessorSupport
        implements InitializationBeanPostProcessor, SmartInstantiationAwareBeanPostProcessor {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  protected Advisor advisor;

  protected boolean beforeExistingAdvisors = false;

  private final ConcurrentHashMap<Class<?>, Boolean> eligibleBeans = new ConcurrentHashMap<>(256);

  /**
   * Set whether this post-processor's advisor is supposed to apply before
   * existing advisors when encountering a pre-advised object.
   * <p>Default is "false", applying the advisor after existing advisors, i.e.
   * as close as possible to the target method. Switch this to "true" in order
   * for this post-processor's advisor to wrap existing advisors as well.
   * <p>Note: Check the concrete post-processor's javadoc whether it possibly
   * changes this flag by default, depending on the nature of its advisor.
   */
  public void setBeforeExistingAdvisors(boolean beforeExistingAdvisors) {
    this.beforeExistingAdvisors = beforeExistingAdvisors;
  }

  @Override
  public Class<?> determineBeanType(Class<?> beanClass, String beanName) {
    if (this.advisor != null && isEligible(beanClass)) {
      ProxyFactory proxyFactory = new ProxyFactory();
      proxyFactory.copyFrom(this);
      proxyFactory.setTargetClass(beanClass);

      if (!proxyFactory.isProxyTargetClass()) {
        evaluateProxyInterfaces(beanClass, proxyFactory);
      }
      proxyFactory.addAdvisor(this.advisor);
      customizeProxyFactory(proxyFactory);

      // Use original ClassLoader if bean class not locally loaded in overriding class loader
      ClassLoader classLoader = getProxyClassLoader();
      if (classLoader instanceof SmartClassLoader smartClassLoader &&
              classLoader != beanClass.getClassLoader()) {
        classLoader = smartClassLoader.getOriginalClassLoader();
      }
      return proxyFactory.getProxyClass(classLoader);
    }

    return beanClass;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (this.advisor == null || bean instanceof AopInfrastructureBean) {
      // Ignore AOP infrastructure such as scoped proxies.
      return bean;
    }

    if (bean instanceof Advised advised
            && !advised.isFrozen() && isEligible(AopUtils.getTargetClass(bean))) {
      // Add our local Advisor to the existing proxy's Advisor chain...
      if (this.beforeExistingAdvisors) {
        advised.addAdvisor(0, this.advisor);
      }
      else if (advised.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE
              && advised.getAdvisorCount() > 0) {
        // No target, leave last advisor in place
        advised.addAdvisor(advised.getAdvisorCount() - 1, this.advisor);
        return bean;
      }
      else {
        advised.addAdvisor(this.advisor);
      }
      return bean;
    }

    if (isEligible(bean, beanName)) {
      ProxyFactory proxyFactory = prepareProxyFactory(bean, beanName);
      if (!proxyFactory.isProxyTargetClass() && !proxyFactory.hasUserSuppliedInterfaces()) {
        evaluateProxyInterfaces(bean.getClass(), proxyFactory);
      }
      proxyFactory.addAdvisor(this.advisor);
      customizeProxyFactory(proxyFactory);
      proxyFactory.setFrozen(isFrozen());
      proxyFactory.setPreFiltered(true);

      // Use original ClassLoader if bean class not locally loaded in overriding class loader
      ClassLoader classLoader = getProxyClassLoader();
      if (classLoader instanceof SmartClassLoader scl && classLoader != bean.getClass().getClassLoader()) {
        classLoader = scl.getOriginalClassLoader();
      }
      return proxyFactory.getProxy(classLoader);
    }

    // No proxy needed.
    return bean;
  }

  /**
   * Check whether the given bean is eligible for advising with this
   * post-processor's {@link Advisor}.
   * <p>Delegates to {@link #isEligible(Class)} for target class checking.
   * Can be overridden e.g. to specifically exclude certain beans by name.
   * <p>Note: Only called for regular bean instances but not for existing
   * proxy instances which implement {@link Advised} and allow for adding
   * the local {@link Advisor} to the existing proxy's {@link Advisor} chain.
   * For the latter, {@link #isEligible(Class)} is being called directly,
   * with the actual target class behind the existing proxy (as determined
   * by {@link AopUtils#getTargetClass(Object)}).
   *
   * @param bean the bean instance
   * @param beanName the name of the bean
   * @see #isEligible(Class)
   */
  protected boolean isEligible(Object bean, String beanName) {
    return isEligible(bean.getClass());
  }

  /**
   * Check whether the given class is eligible for advising with this
   * post-processor's {@link Advisor}.
   * <p>Implements caching of {@code canApply} results per bean target class.
   *
   * @param targetClass the class to check against
   * @see AopUtils#canApply(Advisor, Class)
   */
  protected boolean isEligible(Class<?> targetClass) {
    Boolean eligible = this.eligibleBeans.get(targetClass);
    if (eligible != null) {
      return eligible;
    }
    if (this.advisor == null) {
      return false;
    }
    eligible = AopUtils.canApply(this.advisor, targetClass);
    this.eligibleBeans.put(targetClass, eligible);
    return eligible;
  }

  /**
   * Prepare a {@link ProxyFactory} for the given bean.
   * <p>Subclasses may customize the handling of the target instance and in
   * particular the exposure of the target class. The default introspection
   * of interfaces for non-target-class proxies and the configured advisor
   * will be applied afterwards; {@link #customizeProxyFactory} allows for
   * late customizations of those parts right before proxy creation.
   *
   * @param bean the bean instance to create a proxy for
   * @param beanName the corresponding bean name
   * @return the ProxyFactory, initialized with this processor's
   * {@link ProxyConfig} settings and the specified bean
   * @see #customizeProxyFactory
   */
  protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.copyFrom(this);
    proxyFactory.setTarget(bean);
    proxyFactory.setFrozen(false);
    return proxyFactory;
  }

  /**
   * Subclasses may choose to implement this: for example,
   * to change the interfaces exposed.
   * <p>The default implementation is empty.
   *
   * @param proxyFactory the ProxyFactory that is already configured with
   * target, advisor and interfaces and will be used to create the proxy
   * immediately after this method returns
   * @see #prepareProxyFactory
   */
  protected void customizeProxyFactory(ProxyFactory proxyFactory) {
  }

}
