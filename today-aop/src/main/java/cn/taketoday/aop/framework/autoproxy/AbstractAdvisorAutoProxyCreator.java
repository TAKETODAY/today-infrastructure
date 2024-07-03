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

package cn.taketoday.aop.framework.autoproxy;

import java.util.List;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.framework.AopConfigException;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Generic auto proxy creator that builds AOP proxies for specific beans
 * based on detected Advisors for each bean.
 *
 * <p>Subclasses may override the {@link #findCandidateAdvisors()} method to
 * return a custom list of Advisors applying to any object. Subclasses can
 * also override the inherited {@link #shouldSkip} method to exclude certain
 * objects from auto-proxying.
 *
 * <p>Advisors or advices requiring ordering should be annotated with
 * {@link Order @Order} or implement the
 * {@link cn.taketoday.core.Ordered} interface. This class sorts
 * advisors using the {@link cn.taketoday.core.annotation.AnnotationAwareOrderComparator}. Advisors that are
 * not annotated with {@code @Order} or don't implement the {@code Ordered}
 * interface will be considered as unordered; they will appear at the end of the
 * advisor chain in an undefined order.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Harry Yang
 * @see #findCandidateAdvisors
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {

  @Nullable
  private BeanFactoryAdvisorRetriever advisorRetriever;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    super.setBeanFactory(beanFactory);
    if (!(beanFactory instanceof ConfigurableBeanFactory)) {
      throw new IllegalArgumentException(
              "AdvisorAutoProxyCreator requires a ConfigurableBeanFactory: " + beanFactory);
    }
    initBeanFactory((ConfigurableBeanFactory) beanFactory);
  }

  protected void initBeanFactory(ConfigurableBeanFactory beanFactory) {
    this.advisorRetriever = new BeanFactoryAdvisorRetrieverAdapter(beanFactory);
  }

  @Override
  @Nullable
  protected Object[] getAdvicesAndAdvisorsForBean(
          Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {

    List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
    if (advisors.isEmpty()) {
      return DO_NOT_PROXY;
    }
    return advisors.toArray();
  }

  /**
   * Find all eligible Advisors for auto-proxying this class.
   *
   * @param beanClass the clazz to find advisors for
   * @param beanName the name of the currently proxied bean
   * @return the empty List, not {@code null},
   * if there are no pointcuts or interceptors
   * @see #findCandidateAdvisors
   * @see #sortAdvisors(List)
   * @see #extendAdvisors(List)
   */
  protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
    List<Advisor> candidateAdvisors = findCandidateAdvisors();
    List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
    extendAdvisors(eligibleAdvisors);
    if (!eligibleAdvisors.isEmpty()) {
      try {
        eligibleAdvisors = sortAdvisors(eligibleAdvisors);
      }
      catch (BeanCreationException ex) {
        throw new AopConfigException("Advisor sorting failed with unexpected bean creation, probably due " +
                "to custom use of the Ordered interface. Consider using the @Order annotation instead.", ex);
      }
    }
    return eligibleAdvisors;
  }

  /**
   * Find all candidate Advisors to use in auto-proxying.
   *
   * @return the List of candidate Advisors
   */
  protected List<Advisor> findCandidateAdvisors() {
    Assert.state(this.advisorRetriever != null, "No BeanFactoryAdvisorRetriever available");
    return this.advisorRetriever.retrieveAdvisorBeans();
  }

  /**
   * Search the given candidate Advisors to find all Advisors that
   * can apply to the specified bean.
   *
   * @param candidateAdvisors the candidate Advisors
   * @param beanClass the target's bean class
   * @param beanName the target's bean name
   * @return the List of applicable Advisors
   * @see ProxyCreationContext#getCurrentProxiedBeanName()
   */
  protected List<Advisor> findAdvisorsThatCanApply(
          List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

    ProxyCreationContext.setCurrentProxiedBeanName(beanName);
    try {
      return AopUtils.filterAdvisors(candidateAdvisors, beanClass);
    }
    finally {
      ProxyCreationContext.setCurrentProxiedBeanName(null);
    }
  }

  /**
   * Return whether the Advisor bean with the given name is eligible
   * for proxying in the first place.
   *
   * @param beanName the name of the Advisor bean
   * @return whether the bean is eligible
   */
  protected boolean isEligibleAdvisorBean(String beanName) {
    return true;
  }

  /**
   * Sort advisors based on ordering. Subclasses may choose to override this
   * method to customize the sorting strategy.
   *
   * @param advisors the source List of Advisors
   * @return the sorted List of Advisors
   * @see cn.taketoday.core.Ordered
   * @see Order
   * @see cn.taketoday.core.annotation.AnnotationAwareOrderComparator
   */
  protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
    AnnotationAwareOrderComparator.sort(advisors);
    return advisors;
  }

  /**
   * Extension hook that subclasses can override to register additional Advisors,
   * given the sorted Advisors obtained to date.
   * <p>The default implementation is empty.
   * <p>Typically used to add Advisors that expose contextual information
   * required by some of the later advisors.
   *
   * @param candidateAdvisors the Advisors that have already been identified as
   * applying to a given bean
   */
  protected void extendAdvisors(List<Advisor> candidateAdvisors) { }

  /**
   * This auto-proxy creator always returns pre-filtered Advisors.
   */
  @Override
  protected boolean advisorsPreFiltered() {
    return true;
  }

  /**
   * Subclass of BeanFactoryAdvisorRetrievalHelper that delegates to
   * surrounding AbstractAdvisorAutoProxyCreator facilities.
   */
  private class BeanFactoryAdvisorRetrieverAdapter extends BeanFactoryAdvisorRetriever {

    public BeanFactoryAdvisorRetrieverAdapter(ConfigurableBeanFactory beanFactory) {
      super(beanFactory);
    }

    @Override
    protected boolean isEligibleBean(String beanName) {
      return AbstractAdvisorAutoProxyCreator.this.isEligibleAdvisorBean(beanName);
    }
  }

}
