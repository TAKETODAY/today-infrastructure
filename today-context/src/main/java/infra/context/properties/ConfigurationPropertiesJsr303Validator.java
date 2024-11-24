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

package infra.context.properties;

import infra.context.ApplicationContext;
import infra.util.ClassUtils;
import infra.validation.Errors;
import infra.validation.MessageInterpolatorFactory;
import infra.validation.Validator;
import infra.validation.annotation.Validated;
import infra.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Validator that supports configuration classes annotated with
 * {@link Validated @Validated}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Phillip Webb
 * @since 4.0
 */
final class ConfigurationPropertiesJsr303Validator implements Validator {

  private static final String[] VALIDATOR_CLASSES = {
          "jakarta.validation.Validator",
          "jakarta.validation.ValidatorFactory",
          "jakarta.validation.bootstrap.GenericBootstrap"
  };

  private final Delegate delegate;

  private final Class<?> validatedType;

  ConfigurationPropertiesJsr303Validator(ApplicationContext applicationContext, Class<?> validatedType) {
    this.delegate = new Delegate(applicationContext);
    this.validatedType = validatedType;
  }

  @Override
  public boolean supports(Class<?> type) {
    return this.validatedType.equals(type) && this.delegate.supports(type);
  }

  @Override
  public void validate(Object target, Errors errors) {
    this.delegate.validate(target, errors);
  }

  static boolean isJsr303Present(ApplicationContext applicationContext) {
    ClassLoader classLoader = applicationContext.getClassLoader();
    for (String validatorClass : VALIDATOR_CLASSES) {
      if (!ClassUtils.isPresent(validatorClass, classLoader)) {
        return false;
      }
    }
    return true;
  }

  private static class Delegate extends LocalValidatorFactoryBean {

    Delegate(ApplicationContext applicationContext) {
      setApplicationContext(applicationContext);
      setMessageInterpolator(new MessageInterpolatorFactory(applicationContext).get());
      afterPropertiesSet();
    }

  }

}
