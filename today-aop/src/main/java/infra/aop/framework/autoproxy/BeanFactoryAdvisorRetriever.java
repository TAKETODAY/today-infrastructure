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

package infra.aop.framework.autoproxy;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.aop.Advisor;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanCurrentlyInCreationException;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Helper for retrieving standard Framework Advisors from a BeanFactory,
 * for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @author Harry Yang
 * @see AbstractAdvisorAutoProxyCreator
 * @since 4.0
 */
public class BeanFactoryAdvisorRetriever {

  private static final Logger logger = LoggerFactory.getLogger(BeanFactoryAdvisorRetriever.class);

  private final ConfigurableBeanFactory beanFactory;

  private volatile String @Nullable [] cachedAdvisorBeanNames;

  /**
   * Create a new BeanFactoryAdvisorRetrievalHelper for the given BeanFactory.
   *
   * @param beanFactory the BeanFactory to scan
   */
  public BeanFactoryAdvisorRetriever(ConfigurableBeanFactory beanFactory) {
    Assert.notNull(beanFactory, "ConfigurableBeanFactory is required");
    this.beanFactory = beanFactory;
  }

  /**
   * Find all eligible Advisor beans in the current bean factory,
   * ignoring FactoryBeans and excluding beans that are currently in creation.
   *
   * @return the list of {@link Advisor} beans
   * @see #isEligibleBean
   */
  public List<Advisor> retrieveAdvisorBeans() {
    // Determine list of advisor bean names, if not cached already.
    var advisorNames = this.cachedAdvisorBeanNames;
    if (advisorNames == null) {
      // Do not initialize FactoryBeans here: We need to leave all regular beans
      // uninitialized to let the auto-proxy creator apply to them!
      advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
              beanFactory, Advisor.class, true, false);
      this.cachedAdvisorBeanNames = advisorNames;
    }
    if (advisorNames.length == 0) {
      return new ArrayList<>();
    }

    ArrayList<Advisor> advisors = new ArrayList<>();
    for (String name : advisorNames) {
      if (isEligibleBean(name)) {
        if (beanFactory.isCurrentlyInCreation(name)) {
          if (logger.isTraceEnabled()) {
            logger.trace("Skipping currently created advisor '{}'", name);
          }
        }
        else {
          try {
            advisors.add(beanFactory.getBean(name, Advisor.class));
          }
          catch (BeanCreationException ex) {
            Throwable rootCause = ex.getMostSpecificCause();
            if (rootCause instanceof BeanCurrentlyInCreationException bce) {
              String bceBeanName = bce.getBeanName();
              if (bceBeanName != null && beanFactory.isCurrentlyInCreation(bceBeanName)) {
                if (logger.isTraceEnabled()) {
                  logger.trace("Skipping advisor '{}' with dependency on currently created bean: {}",
                          name, ex.getMessage());
                }
                // Ignore: indicates a reference back to the bean we're trying to advise.
                // We want to find advisors other than the currently created bean itself.
                continue;
              }
            }
            throw ex;
          }
        }
      }
    }
    return advisors;
  }

  /**
   * Determine whether the aspect bean with the given name is eligible.
   * <p>The default implementation always returns {@code true}.
   *
   * @param beanName the name of the aspect bean
   * @return whether the bean is eligible
   */
  protected boolean isEligibleBean(String beanName) {
    return true;
  }

}
