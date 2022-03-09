/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.validation.beanvalidation;

import java.util.Iterator;
import java.util.Set;

import cn.taketoday.aop.framework.AopProxyUtils;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanInitializationException;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Simple {@link InitializationBeanPostProcessor} that checks JSR-303 constraint annotations
 * in Framework-managed beans, throwing an initialization exception in case of
 * constraint violations right before calling the bean's init method (if any).
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class BeanValidationPostProcessor implements InitializationBeanPostProcessor, InitializingBean {

  @Nullable
  private Validator validator;

  private boolean afterInitialization = false;

  /**
   * Set the JSR-303 Validator to delegate to for validating beans.
   * <p>Default is the default ValidatorFactory's default Validator.
   */
  public void setValidator(Validator validator) {
    this.validator = validator;
  }

  /**
   * Set the JSR-303 ValidatorFactory to delegate to for validating beans,
   * using its default Validator.
   * <p>Default is the default ValidatorFactory's default Validator.
   *
   * @see ValidatorFactory#getValidator()
   */
  public void setValidatorFactory(ValidatorFactory validatorFactory) {
    this.validator = validatorFactory.getValidator();
  }

  /**
   * Choose whether to perform validation after bean initialization
   * (i.e. after init methods) instead of before (which is the default).
   * <p>Default is "false" (before initialization). Switch this to "true"
   * (after initialization) if you would like to give init methods a chance
   * to populate constrained fields before they get validated.
   */
  public void setAfterInitialization(boolean afterInitialization) {
    this.afterInitialization = afterInitialization;
  }

  @Override
  public void afterPropertiesSet() {
    if (this.validator == null) {
      this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (!this.afterInitialization) {
      doValidate(bean);
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (this.afterInitialization) {
      doValidate(bean);
    }
    return bean;
  }

  /**
   * Perform validation of the given bean.
   *
   * @param bean the bean instance to validate
   * @see Validator#validate
   */
  protected void doValidate(Object bean) {
    Assert.state(this.validator != null, "No Validator set");
    Object objectToValidate = AopProxyUtils.getSingletonTarget(bean);
    if (objectToValidate == null) {
      objectToValidate = bean;
    }
    Set<ConstraintViolation<Object>> result = this.validator.validate(objectToValidate);

    if (!result.isEmpty()) {
      StringBuilder sb = new StringBuilder("Bean state is invalid: ");
      Iterator<ConstraintViolation<Object>> it = result.iterator();
      while (it.hasNext()) {
        ConstraintViolation<Object> violation = it.next();
        sb.append(violation.getPropertyPath())
                .append(" - ")
                .append(violation.getMessage());
        if (it.hasNext()) {
          sb.append("; ");
        }
      }
      throw new BeanInitializationException(sb.toString());
    }
  }

}
