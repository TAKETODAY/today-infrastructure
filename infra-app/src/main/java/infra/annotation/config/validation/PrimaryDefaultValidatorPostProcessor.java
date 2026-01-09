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

package infra.annotation.config.validation;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.config.BeanDefinition;
import infra.context.BootstrapContext;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.core.type.AnnotationMetadata;
import infra.validation.Validator;
import infra.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Enable the {@code Primary} flag on the auto-configured validator if necessary.
 * <p>
 * As {@link LocalValidatorFactoryBean} exposes 3 validator related contracts and we're
 * only checking for the absence {@link jakarta.validation.Validator}, we should flag the
 * auto-configured validator as primary only if no Infra {@link Validator} is flagged
 * as primary.
 *
 * @author Stephane Nicoll
 * @author Matej Nedic
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class PrimaryDefaultValidatorPostProcessor implements ImportBeanDefinitionRegistrar {

  /**
   * The bean name of the auto-configured Validator.
   */
  private static final String VALIDATOR_BEAN_NAME = "defaultValidator";

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BootstrapContext context) {
    BeanDefinition definition = getAutoConfiguredValidator(context);
    if (definition != null) {
      definition.setPrimary(!hasPrimaryInfraValidator(context.getBeanFactory()));
    }
  }

  @Nullable
  private BeanDefinition getAutoConfiguredValidator(BootstrapContext context) {
    if (context.containsBeanDefinition(VALIDATOR_BEAN_NAME)) {
      BeanFactory beanFactory = context.getBeanFactory();
      BeanDefinition definition = context.getBeanDefinition(VALIDATOR_BEAN_NAME);
      if (definition.getRole() == BeanDefinition.ROLE_INFRASTRUCTURE
              && isTypeMatch(beanFactory, VALIDATOR_BEAN_NAME, LocalValidatorFactoryBean.class)) {
        return definition;
      }
    }
    return null;
  }

  private boolean isTypeMatch(BeanFactory beanFactory, String name, Class<?> type) {
    return beanFactory != null && beanFactory.isTypeMatch(name, type);
  }

  private boolean hasPrimaryInfraValidator(BeanFactory beanFactory) {
    var validatorBeans = beanFactory.getBeanNamesForType(Validator.class, false, false);
    for (String validatorBean : validatorBeans) {
      BeanDefinition definition = beanFactory.getBeanDefinition(validatorBean);
      if (definition.isPrimary()) {
        return true;
      }
    }
    return false;
  }

}
