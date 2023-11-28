/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.validation;

import java.util.Set;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.validation.Validator;
import cn.taketoday.validation.beanvalidation.LocalValidatorFactoryBean;

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
    Set<String> validatorBeans = beanFactory.getBeanNamesForType(Validator.class, false, false);
    for (String validatorBean : validatorBeans) {
      BeanDefinition definition = beanFactory.getBeanDefinition(validatorBean);
      if (definition.isPrimary()) {
        return true;
      }
    }
    return false;
  }

}
