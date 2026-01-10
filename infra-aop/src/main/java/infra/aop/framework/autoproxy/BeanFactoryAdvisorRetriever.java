/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
