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

package infra.validation.beanvalidation;

import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Set;

import infra.aop.framework.AopProxyUtils;
import infra.beans.BeansException;
import infra.beans.factory.BeanInitializationException;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.InitializingBean;
import infra.lang.Assert;
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0s
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
