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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cn.taketoday.aop.Advisor;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanCurrentlyInCreationException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

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

  @Nullable
  private volatile Set<String> cachedAdvisorBeanNames;

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
    Set<String> advisorNames = this.cachedAdvisorBeanNames;
    if (advisorNames == null) {
      // Do not initialize FactoryBeans here: We need to leave all regular beans
      // uninitialized to let the auto-proxy creator apply to them!
      advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
              beanFactory, Advisor.class, true, false);
      this.cachedAdvisorBeanNames = advisorNames;
    }
    if (advisorNames.isEmpty()) {
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
            if (rootCause instanceof BeanCurrentlyInCreationException) {
              BeanCreationException bce = (BeanCreationException) rootCause;
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
