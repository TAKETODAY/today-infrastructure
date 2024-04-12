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

package cn.taketoday.validation.beanvalidation;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Nullable;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorContext;
import jakarta.validation.ValidatorFactory;

/**
 * Configurable bean class that exposes a specific JSR-303 Validator
 * through its original interface as well as through the Framework
 * {@link cn.taketoday.validation.Validator} interface.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CustomValidatorBean extends InfraValidatorAdapter implements Validator, InitializingBean {

  @Nullable
  private ValidatorFactory validatorFactory;

  @Nullable
  private MessageInterpolator messageInterpolator;

  @Nullable
  private TraversableResolver traversableResolver;

  /**
   * Set the ValidatorFactory to obtain the target Validator from.
   * <p>Default is {@link Validation#buildDefaultValidatorFactory()}.
   */
  public void setValidatorFactory(ValidatorFactory validatorFactory) {
    this.validatorFactory = validatorFactory;
  }

  /**
   * Specify a custom MessageInterpolator to use for this Validator.
   */
  public void setMessageInterpolator(MessageInterpolator messageInterpolator) {
    this.messageInterpolator = messageInterpolator;
  }

  /**
   * Specify a custom TraversableResolver to use for this Validator.
   */
  public void setTraversableResolver(TraversableResolver traversableResolver) {
    this.traversableResolver = traversableResolver;
  }

  @Override
  public void afterPropertiesSet() {
    if (this.validatorFactory == null) {
      this.validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    ValidatorContext validatorContext = this.validatorFactory.usingContext();
    MessageInterpolator targetInterpolator = this.messageInterpolator;
    if (targetInterpolator == null) {
      targetInterpolator = this.validatorFactory.getMessageInterpolator();
    }
    validatorContext.messageInterpolator(new LocaleContextMessageInterpolator(targetInterpolator));
    if (this.traversableResolver != null) {
      validatorContext.traversableResolver(this.traversableResolver);
    }

    setTargetValidator(validatorContext.getValidator());
  }

}
