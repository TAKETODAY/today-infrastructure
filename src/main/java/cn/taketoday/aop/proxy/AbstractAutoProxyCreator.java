/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.aop.proxy;

import org.aopalliance.aop.Advice;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.AopInfrastructureBean;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.aop.target.TargetSourceCreator;
import cn.taketoday.context.aware.Aware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.factory.InstantiationAwareBeanPostProcessor;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;

/**
 * Abstract Auto Proxy Creator use {@link cn.taketoday.context.factory.BeanPostProcessor}
 * mechanism to replace original bean
 *
 * @author TODAY 2021/2/1 21:31
 * @since 3.0
 */
public abstract class AbstractAutoProxyCreator
        extends ProxyConfig implements InstantiationAwareBeanPostProcessor, BeanFactoryAware, AopInfrastructureBean, ProxyCreator {

  private static final long serialVersionUID = 1L;

  protected final transient Logger log = LoggerFactory.getLogger(getClass());

  private BeanFactory beanFactory;

  private static final Object[] DO_NOT_PROXY = null;

  /**
   * Indicates whether or not the proxy should be frozen. Overridden from super
   * to prevent the configuration from becoming frozen too early.
   */
  private boolean freezeProxy = false;
  private transient TargetSourceCreator[] targetSourceCreators;
  private transient ClassLoader proxyClassLoader = ClassUtils.getClassLoader();
  private List<Advisor> candidateAdvisors;

  /**
   * Set custom {@code TargetSourceCreators} to be applied in this order.
   * If the list is empty, or they all return null, a {@link SingletonTargetSource}
   * will be created for each bean.
   * <p>Note that TargetSourceCreators will kick in even for target beans
   * where no advices or advisors have been found. If a {@code TargetSourceCreator}
   * returns a {@link TargetSource} for a specific bean, that bean will be proxied
   * in any case.
   * <p>{@code TargetSourceCreators} can only be invoked if this post processor is used
   * in a {@link BeanFactory} and its {@link BeanFactoryAware} callback is triggered.
   *
   * @param targetSourceCreators
   *         the list of {@code TargetSourceCreators}.
   *         Ordering is significant: The {@code TargetSource} returned from the first matching
   *         {@code TargetSourceCreator} (that is, the first that returns non-null) will be used.
   */
  public void setTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
    this.targetSourceCreators = targetSourceCreators;
  }

  /**
   * Set whether or not the proxy should be frozen, preventing advice
   * from being added to it once it is created.
   * <p>Overridden from the super class to prevent the proxy configuration
   * from being frozen before the proxy is created.
   */
  @Override
  public void setFrozen(boolean frozen) {
    this.freezeProxy = frozen;
  }

  @Override
  public boolean isFrozen() {
    return this.freezeProxy;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  protected BeanFactory getBeanFactory() {
    return this.beanFactory;
  }

  public void setProxyClassLoader(ClassLoader proxyClassLoader) {
    this.proxyClassLoader = proxyClassLoader;
  }

  private ClassLoader getProxyClassLoader() {
    return proxyClassLoader;
  }

  /**
   * Use TargetSourceCreator to create TargetSource
   *
   * @param def
   *         the BeanDefinition of the bean to be instantiated
   *
   * @return a proxy bean or null
   */
  @Override
  public Object postProcessBeforeInstantiation(final BeanDefinition def) {
    // Create proxy here if we have a custom TargetSource.
    // Suppresses unnecessary default instantiation of the target bean:
    // The TargetSource will handle target instances in a custom fashion.
    TargetSource targetSource = getCustomTargetSource(def);
    if (targetSource != null) {
      Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(def, targetSource);
      if (ObjectUtils.isNotEmpty(specificInterceptors)) {
        return createProxy(def, specificInterceptors, targetSource);
      }
    }
    return null;
  }

  protected TargetSource getCustomTargetSource(BeanDefinition def) {
    // We can't create fancy target sources for directly registered singletons.
    if (this.targetSourceCreators != null) {
      for (TargetSourceCreator creator : this.targetSourceCreators) {
        TargetSource source = creator.getTargetSource(def);
        if (source != null) {
          // Found a matching TargetSource.
          if (log.isTraceEnabled()) {
            log.trace("TargetSourceCreator [{}] found custom TargetSource for bean with BeanDefinition '{}'",
                      creator, def);
          }
          return source;
        }
      }
    }
    // No custom TargetSource found.
    return null;
  }

  /**
   * Create a proxy with the configured interceptors if the bean is
   * identified as one to proxy by the subclass.
   *
   * @see #getAdvicesAndAdvisorsForBean
   */
  @Override
  public Object postProcessAfterInitialization(Object bean, BeanDefinition def) {
    return wrapIfNecessary(bean, def);
  }

  protected boolean advisorsPreFiltered() {
    return false;
  }

  protected void customizeProxyFactory(ProxyFactory proxyFactory) {

  }

  protected Advisor[] getAdvisors(BeanDefinition def, Object[] specificInterceptors) {
    Advisor[] ret = new Advisor[specificInterceptors.length];
    int i = 0;
    for (final Object specificInterceptor : specificInterceptors) {
      ret[i++] = AopUtils.wrap(specificInterceptor);
    }
    return ret;
  }

  protected Object wrapIfNecessary(Object bean, BeanDefinition def) {
    if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean, def)) {
      return bean;
    }
    // Create proxy if we have advice.
    final TargetSource targetSource = getTargetSource(bean, def);
    Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(def, targetSource);
    if (ObjectUtils.isNotEmpty(specificInterceptors)) {
      return createProxy(def, specificInterceptors, targetSource);
    }
    return bean;
  }

  protected TargetSource getTargetSource(Object bean, BeanDefinition def) {
    TargetSource targetSource = getCustomTargetSource(def);
    if (targetSource == null) {
      targetSource = new SingletonTargetSource(bean);
    }
    return targetSource;
  }

  protected Object createProxy(BeanDefinition def, Object[] specificInterceptors, TargetSource targetSource) {

    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.copyFrom(this);

    if (!proxyFactory.isProxyTargetClass()) {
      if (shouldProxyTargetClass(def)) {
        proxyFactory.setProxyTargetClass(true);
      }
      else {
        evaluateProxyInterfaces(def.getBeanClass(), proxyFactory);
      }
    }

    Advisor[] advisors = getAdvisors(def, specificInterceptors);
    proxyFactory.addAdvisors(advisors);
    proxyFactory.setTargetSource(targetSource);
    customizeProxyFactory(proxyFactory);

    proxyFactory.setFrozen(this.freezeProxy);
    if (advisorsPreFiltered()) {
      proxyFactory.setPreFiltered(true);
    }

    final class ParametersFunction implements Function<Constructor<?>, Object[]> {

      @Override
      public Object[] apply(Constructor<?> constructor) {
        return ContextUtils.resolveParameter(constructor, beanFactory);
      }
    }

    return proxyFactory.getProxy(getProxyClassLoader(), new ParametersFunction());
  }

  protected Object[] getAdvicesAndAdvisorsForBean(BeanDefinition def, TargetSource targetSource) {
    final List<Advisor> candidateAdvisors = getCandidateAdvisors();
    List<Advisor> eligibleAdvisors = filterAdvisors(candidateAdvisors, def, targetSource);
    postEligibleAdvisors(eligibleAdvisors);

    eligibleAdvisors = sortAdvisors(eligibleAdvisors);
    if (eligibleAdvisors.isEmpty()) {
      return DO_NOT_PROXY;
    }
    return eligibleAdvisors.toArray();
  }

  protected List<Advisor> filterAdvisors(final List<Advisor> candidateAdvisors,
                                         BeanDefinition def, TargetSource targetSource) {
    return AopUtils.filterAdvisors(candidateAdvisors, def.getBeanClass());
  }

  protected List<Advisor> getCandidateAdvisors() {
    if (candidateAdvisors == null) {
      candidateAdvisors = new ArrayList<>();
      addCandidateAdvisors(candidateAdvisors);
    }
    return candidateAdvisors;
  }

  protected void addCandidateAdvisors(List<Advisor> candidateAdvisors) {
    final BeanFactory beanFactory = getBeanFactory();
    candidateAdvisors.addAll(beanFactory.getBeans(Advisor.class));
  }

  /**
   * Extension hook that subclasses can override to register additional Advisors,
   * given the sorted Advisors obtained to date.
   * <p>The default implementation is empty.
   * <p>Typically used to add Advisors that expose contextual information
   * required by some of the later advisors.
   *
   * @param eligibleAdvisors
   *         the Advisors that have already been identified as
   *         applying to a given bean
   */
  protected void postEligibleAdvisors(List<Advisor> eligibleAdvisors) { }

  /**
   * Sort advisors based on ordering. Subclasses may choose to override this
   * method to customize the sorting strategy.
   *
   * @param advisors
   *         the source List of Advisors
   *
   * @return the sorted List of Advisors
   *
   * @see cn.taketoday.context.Ordered
   * @see cn.taketoday.context.annotation.Order
   * @see OrderUtils
   */
  protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
    return OrderUtils.reversedSort(advisors);
  }

  protected boolean shouldSkip(Object bean, BeanDefinition def) {
    return false;
  }

  /**
   * Return whether the given bean class represents an infrastructure class
   * that should never be proxied.
   * <p>The default implementation considers Advices, Advisors and
   * AopInfrastructureBeans as infrastructure classes.
   *
   * @param beanClass
   *         the class of the bean
   *
   * @return whether the bean represents an infrastructure class
   *
   * @see org.aopalliance.aop.Advice
   * @see cn.taketoday.aop.Advisor
   * @see cn.taketoday.aop.AopInfrastructureBean
   */
  protected boolean isInfrastructureClass(Class<?> beanClass) {
    return Advice.class.isAssignableFrom(beanClass)
            || Advisor.class.isAssignableFrom(beanClass)
            || Pointcut.class.isAssignableFrom(beanClass)
            || AopInfrastructureBean.class.isAssignableFrom(beanClass);
  }

  /**
   * Check the interfaces on the given bean class and apply them to the
   * {@link ProxyFactory}, if appropriate.
   * <p>Calls {@link #isConfigurationCallbackInterface} to filter for reasonable
   * proxy interfaces, falling back to a target-class proxy otherwise.
   *
   * @param beanClass
   *         the class of the bean
   * @param proxyFactory
   *         the ProxyFactory for the bean
   */
  protected void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
    Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, getProxyClassLoader());
    boolean hasReasonableProxyInterface = false;
    for (Class<?> ifc : targetInterfaces) {
      if (!isConfigurationCallbackInterface(ifc) && ifc.getMethods().length > 0) {
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
   * @param ifc
   *         the interface to check
   *
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
   * Determine whether the given bean should be proxied with its target class rather than its interfaces.
   *
   * @param def
   *         the BeanDefinition of the bean
   *
   * @return whether the given bean should be proxied with its target class
   */
  protected boolean shouldProxyTargetClass(BeanDefinition def) {
//    return def.getAttribute();
    return false;
  }

}
