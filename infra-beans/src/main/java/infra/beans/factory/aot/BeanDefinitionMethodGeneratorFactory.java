/*
 * Copyright 2002-present the original author or authors.
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

package infra.beans.factory.aot;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.beans.factory.aot.AotServices.Source;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.RegisteredBean;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ObjectUtils;

/**
 * Factory used to create a {@link BeanDefinitionMethodGenerator} instance for a
 * {@link RegisteredBean}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanDefinitionMethodGenerator
 * @see #getBeanDefinitionMethodGenerator(RegisteredBean)
 * @since 4.0
 */
class BeanDefinitionMethodGeneratorFactory {

  private static final Logger logger = LoggerFactory.getLogger(BeanDefinitionMethodGeneratorFactory.class);

  private final AotServices<BeanRegistrationAotProcessor> aotProcessors;

  private final AotServices<BeanRegistrationExcludeFilter> excludeFilters;

  /**
   * Create a new {@link BeanDefinitionMethodGeneratorFactory} backed by the
   * given {@link ConfigurableBeanFactory}.
   *
   * @param beanFactory the bean factory use
   */
  BeanDefinitionMethodGeneratorFactory(ConfigurableBeanFactory beanFactory) {
    this(AotServices.factoriesAndBeans(beanFactory));
  }

  /**
   * Create a new {@link BeanDefinitionMethodGeneratorFactory} backed by the
   * given {@link AotServices.Loader}.
   *
   * @param loader the AOT services loader to use
   */
  BeanDefinitionMethodGeneratorFactory(AotServices.Loader loader) {
    this.aotProcessors = loader.load(BeanRegistrationAotProcessor.class);
    this.excludeFilters = loader.load(BeanRegistrationExcludeFilter.class);
    for (BeanRegistrationExcludeFilter excludeFilter : this.excludeFilters) {
      if (this.excludeFilters.getSource(excludeFilter) == Source.BEAN_FACTORY) {
        Assert.state(excludeFilter instanceof BeanRegistrationAotProcessor
                        || excludeFilter instanceof BeanFactoryInitializationAotProcessor,
                () -> "BeanRegistrationExcludeFilter bean of type %s must also implement an AOT processor interface"
                        .formatted(excludeFilter.getClass().getName()));
      }
    }
  }

  /**
   * Return a {@link BeanDefinitionMethodGenerator} for the given
   * {@link RegisteredBean} defined with the specified property name, or
   * {@code null} if the registered bean is excluded by a
   * {@link BeanRegistrationExcludeFilter}. The resulting
   * {@link BeanDefinitionMethodGenerator} will include all
   * {@link BeanRegistrationAotProcessor} provided contributions.
   *
   * @param registeredBean the registered bean
   * @param currentPropertyName the property name that this bean belongs to
   * @return a new {@link BeanDefinitionMethodGenerator} instance or {@code null}
   */
  @Nullable
  BeanDefinitionMethodGenerator getBeanDefinitionMethodGenerator(
          RegisteredBean registeredBean, @Nullable String currentPropertyName) {

    if (isExcluded(registeredBean)) {
      return null;
    }
    List<BeanRegistrationAotContribution> contributions = getAotContributions(registeredBean);
    return new BeanDefinitionMethodGenerator(this, registeredBean,
            currentPropertyName, contributions);
  }

  /**
   * Return a {@link BeanDefinitionMethodGenerator} for the given
   * {@link RegisteredBean} or {@code null} if the registered bean is excluded
   * by a {@link BeanRegistrationExcludeFilter}. The resulting
   * {@link BeanDefinitionMethodGenerator} will include all
   * {@link BeanRegistrationAotProcessor} provided contributions.
   *
   * @param registeredBean the registered bean
   * @return a new {@link BeanDefinitionMethodGenerator} instance or {@code null}
   */
  @Nullable
  BeanDefinitionMethodGenerator getBeanDefinitionMethodGenerator(RegisteredBean registeredBean) {
    return getBeanDefinitionMethodGenerator(registeredBean, null);
  }

  private boolean isExcluded(RegisteredBean registeredBean) {
    if (isImplicitlyExcluded(registeredBean)) {
      return true;
    }
    for (BeanRegistrationExcludeFilter excludeFilter : this.excludeFilters) {
      if (excludeFilter.isExcludedFromAotProcessing(registeredBean)) {
        logger.trace("Excluding registered bean '{}' from bean factory {} due to {}",
                registeredBean.getBeanName(),
                ObjectUtils.identityToString(registeredBean.getBeanFactory()),
                excludeFilter.getClass().getName());
        return true;
      }
    }
    return false;
  }

  private boolean isImplicitlyExcluded(RegisteredBean registeredBean) {
    if (Boolean.TRUE.equals(registeredBean.getMergedBeanDefinition()
            .getAttribute(BeanRegistrationAotProcessor.IGNORE_REGISTRATION_ATTRIBUTE))) {
      return true;
    }
    Class<?> beanClass = registeredBean.getBeanClass();
    if (BeanFactoryInitializationAotProcessor.class.isAssignableFrom(beanClass)) {
      return true;
    }
    if (BeanRegistrationAotProcessor.class.isAssignableFrom(beanClass)) {
      BeanRegistrationAotProcessor processor = this.aotProcessors.findByBeanName(registeredBean.getBeanName());
      return (processor == null || processor.isBeanExcludedFromAotProcessing());
    }
    return false;
  }

  private List<BeanRegistrationAotContribution> getAotContributions(RegisteredBean registeredBean) {
    String beanName = registeredBean.getBeanName();
    List<BeanRegistrationAotContribution> contributions = new ArrayList<>();
    for (BeanRegistrationAotProcessor aotProcessor : this.aotProcessors) {
      BeanRegistrationAotContribution contribution = aotProcessor.processAheadOfTime(registeredBean);
      if (contribution != null) {
        logger.trace("Adding bean registration AOT contribution {} from {} to '{}'",
                contribution.getClass().getName(),
                aotProcessor.getClass().getName(), beanName);
        contributions.add(contribution);
      }
    }
    return contributions;
  }

}
