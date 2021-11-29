/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.AopInfrastructureBean;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.aop.target.TargetSourceCreator;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeansException;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.InstantiationAwareBeanPostProcessor;
import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.OrderUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;

/**
 * Abstract Auto Proxy Creator use {@link cn.taketoday.beans.factory.BeanPostProcessor}
 * mechanism to replace original bean
 *
 * @author TODAY 2021/2/1 21:31
 * @since 3.0
 */
public abstract class AbstractAutoProxyCreator
        extends ProxyProcessorSupport
        implements InstantiationAwareBeanPostProcessor,
                   BeanFactoryAware, AopInfrastructureBean,
                   ProxyCreator, InitializationBeanPostProcessor {
  @Serial
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
   * @param targetSourceCreators the list of {@code TargetSourceCreators}.
   * Ordering is significant: The {@code TargetSource} returned from the first matching
   * {@code TargetSourceCreator} (that is, the first that returns non-null) will be used.
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

  public BeanFactory getBeanFactory() {
    return this.beanFactory;
  }

  @Override
  public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
    // Create proxy here if we have a custom TargetSource.
    // Suppresses unnecessary default instantiation of the target bean:
    // The TargetSource will handle target instances in a custom fashion.
    TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
    if (targetSource != null) {
      Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
      if (ObjectUtils.isNotEmpty(specificInterceptors)) {
        return createProxy(beanClass, beanName, specificInterceptors, targetSource);
      }
    }
    return null;
  }

  protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
    // We can't create fancy target sources for directly registered singletons.
    if (this.targetSourceCreators != null) {
      for (TargetSourceCreator creator : this.targetSourceCreators) {
        TargetSource source = creator.getTargetSource(beanClass, beanName);
        if (source != null) {
          // Found a matching TargetSource.
          if (log.isTraceEnabled()) {
            log.trace("TargetSourceCreator [{}] found custom TargetSource for bean with name '{}'",
                      creator, beanName);
          }
          return source;
        }
      }
    }
    // No custom TargetSource found.
    return null;
  }

  @Override
  public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
    return wrapIfNecessary(bean, beanName);
  }

  /**
   * Create a proxy with the configured interceptors if the bean is
   * identified as one to proxy by the subclass.
   *
   * @see #getAdvicesAndAdvisorsForBean
   */
  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    return wrapIfNecessary(bean, beanName);
  }

  protected boolean advisorsPreFiltered() {
    return false;
  }

  protected void customizeProxyFactory(ProxyFactory proxyFactory) {

  }

  protected Advisor[] getAdvisors(Class<?> beanClass, Object[] specificInterceptors) {
    Advisor[] ret = new Advisor[specificInterceptors.length];
    int i = 0;
    for (Object specificInterceptor : specificInterceptors) {
      ret[i++] = AopUtils.wrap(specificInterceptor);
    }
    return ret;
  }

  protected Object wrapIfNecessary(Object bean, String beanName) {
    Class<?> beanClass = bean.getClass();
    if (isInfrastructureClass(beanClass) || shouldSkip(bean, beanName)) {
      return bean;
    }
    // Create proxy if we have advice.
    TargetSource targetSource = getTargetSource(bean, beanName);
    Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
    if (ObjectUtils.isNotEmpty(specificInterceptors)) {
      return createProxy(beanClass, beanName, specificInterceptors, targetSource);
    }
    return bean;
  }

  protected TargetSource getTargetSource(Object bean, String def) {
    TargetSource targetSource = getCustomTargetSource(bean.getClass(), def);
    if (targetSource == null) {
      targetSource = new SingletonTargetSource(bean);
    }
    return targetSource;
  }

  protected Object createProxy(
          Class<?> beanClass, String beanName, Object[] specificInterceptors, TargetSource targetSource) {

    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.copyFrom(this);

    if (!proxyFactory.isProxyTargetClass()) {
      if (shouldProxyTargetClass(beanClass, beanName)) {
        proxyFactory.setProxyTargetClass(true);
      }
      else {
        evaluateProxyInterfaces(beanClass, proxyFactory);
      }
    }

    Advisor[] advisors = getAdvisors(beanClass, specificInterceptors);
    proxyFactory.addAdvisors(advisors);
    proxyFactory.setTargetSource(targetSource);
    customizeProxyFactory(proxyFactory);

    proxyFactory.setFrozen(this.freezeProxy);
    if (advisorsPreFiltered()) {
      proxyFactory.setPreFiltered(true);
    }

    return proxyFactory.getProxy(getProxyClassLoader());
  }

  /**
   * Return whether the given bean is to be proxied, what additional
   * advices (e.g. AOP Alliance interceptors) and advisors to apply.
   *
   * @param beanClass the class of the bean to advise
   * @param beanName the name of the bean
   * @param targetSource the TargetSource returned by the
   * {@link #getCustomTargetSource} method: may be ignored.
   * Will be {@code null} if no custom target source is in use.
   * @return an array of additional interceptors for the particular bean;
   * or an empty array if no additional interceptors but just the common ones;
   * or {@code null} if no proxy at all, not even with the common interceptors.
   * See constants DO_NOT_PROXY and PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS.
   * @throws BeansException in case of errors
   * @see #DO_NOT_PROXY
   */
  protected Object[] getAdvicesAndAdvisorsForBean(
          Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {
    List<Advisor> candidateAdvisors = getCandidateAdvisors();
    List<Advisor> eligibleAdvisors = filterAdvisors(candidateAdvisors, beanClass, targetSource);
    postEligibleAdvisors(eligibleAdvisors);

    // sort advisers
    sortAdvisors(eligibleAdvisors);

    if (eligibleAdvisors.isEmpty()) {
      return DO_NOT_PROXY;
    }
    return eligibleAdvisors.toArray();
  }

  protected List<Advisor> filterAdvisors(
          List<Advisor> candidateAdvisors, Class<?> beanClass, TargetSource targetSource) {
    return AopUtils.filterAdvisors(candidateAdvisors, beanClass);
  }

  protected List<Advisor> getCandidateAdvisors() {
    if (candidateAdvisors == null) {
      candidateAdvisors = new ArrayList<>();
      addCandidateAdvisors(candidateAdvisors);
    }
    return candidateAdvisors;
  }

  protected void addCandidateAdvisors(List<Advisor> candidateAdvisors) {
    BeanFactory beanFactory = getBeanFactory();
    candidateAdvisors.addAll(beanFactory.getBeans(Advisor.class));
  }

  /**
   * Extension hook that subclasses can override to register additional Advisors,
   * given the sorted Advisors obtained to date.
   * <p>The default implementation is empty.
   * <p>Typically used to add Advisors that expose contextual information
   * required by some of the later advisors.
   *
   * @param eligibleAdvisors the Advisors that have already been identified as
   * applying to a given bean
   */
  protected void postEligibleAdvisors(List<Advisor> eligibleAdvisors) { }

  /**
   * Sort advisors based on ordering. Subclasses may choose to override this
   * method to customize the sorting strategy.
   *
   * @param advisors the source List of Advisors
   * @see Ordered
   * @see Order
   * @see OrderUtils
   */
  protected void sortAdvisors(List<Advisor> advisors) {
    AnnotationAwareOrderComparator.sort(advisors);
  }

  protected boolean shouldSkip(Object bean, String def) {
    return false;
  }

  /**
   * Return whether the given bean class represents an infrastructure class
   * that should never be proxied.
   * <p>The default implementation considers Advices, Advisors and
   * AopInfrastructureBeans as infrastructure classes.
   *
   * @param beanClass the class of the bean
   * @return whether the bean represents an infrastructure class
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
   * Determine whether the given bean should be proxied with its target class rather than its interfaces.
   *
   * @param beanClass the class of the bean
   * @param beanName the name of the bean
   * @return whether the given bean should be proxied with its target class
   */
  protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
    return false;
  }

}
