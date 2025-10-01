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

package infra.annotation.config.validation;

import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.MessageSource;
import infra.validation.Errors;
import infra.validation.MessageInterpolatorFactory;
import infra.validation.SmartValidator;
import infra.validation.Validator;
import infra.validation.beanvalidation.InfraValidatorAdapter;
import infra.validation.beanvalidation.OptionalValidatorFactoryBean;
import jakarta.validation.ValidationException;

/**
 * {@link Validator} implementation that delegates calls to another {@link Validator}.
 * This {@link Validator} implements Infra {@link SmartValidator} interface but does
 * not implement the JSR-303 {@code jakarta.validator.Validator} interface.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ValidatorAdapter implements SmartValidator, ApplicationContextAware, InitializingBean, DisposableBean {

  private final SmartValidator target;

  private final boolean existingBean;

  ValidatorAdapter(SmartValidator target, boolean existingBean) {
    this.target = target;
    this.existingBean = existingBean;
  }

  public final Validator getTarget() {
    return this.target;
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return this.target.supports(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    this.target.validate(target, errors);
  }

  @Override
  public void validate(Object target, Errors errors, Object... validationHints) {
    this.target.validate(target, errors, validationHints);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    if (!this.existingBean && this.target instanceof ApplicationContextAware contextAwareTarget) {
      contextAwareTarget.setApplicationContext(applicationContext);
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (!this.existingBean && this.target instanceof InitializingBean initializingBean) {
      initializingBean.afterPropertiesSet();
    }
  }

  @Override
  public void destroy() throws Exception {
    if (!this.existingBean && this.target instanceof DisposableBean disposableBean) {
      disposableBean.destroy();
    }
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrap(@Nullable Class<T> type) {
    if (type != null) {
      if (type.isInstance(this.target)) {
        return (T) this.target;
      }
      return this.target.unwrap(type);
    }
    return null;
  }

  /**
   * Return a {@link Validator} that only implements the {@link Validator} interface,
   * wrapping it if necessary.
   * <p>
   * If the specified {@link Validator} is not {@code null}, it is wrapped. If not, a
   * {@link jakarta.validation.Validator} is retrieved from the context and wrapped.
   * Otherwise, a new default validator is created.
   *
   * @param applicationContext the application context
   * @param validator an existing validator to use or {@code null}
   * @return the validator to use
   */
  public static Validator get(ApplicationContext applicationContext, @Nullable Validator validator) {
    if (validator != null) {
      return wrap(validator, false);
    }

    Validator existing = getExisting(applicationContext);
    if (existing != null) {
      return wrap(existing, true);
    }
    return create(applicationContext);
  }

  @Nullable
  private static Validator getExisting(ApplicationContext context) {
    try {
      var validatorBean = context.getBean(jakarta.validation.Validator.class);
      if (validatorBean instanceof Validator validator) {
        return validator;
      }
      return new InfraValidatorAdapter(validatorBean);
    }
    catch (NoSuchBeanDefinitionException ex) {
      return null;
    }
  }

  private static Validator create(MessageSource messageSource) {
    OptionalValidatorFactoryBean validator = new OptionalValidatorFactoryBean();
    try {
      MessageInterpolatorFactory factory = new MessageInterpolatorFactory(messageSource);
      validator.setMessageInterpolator(factory.get());
    }
    catch (ValidationException ex) {
      // Ignore
    }
    return wrap(validator, false);
  }

  private static Validator wrap(Validator validator, boolean existingBean) {
    if (validator instanceof jakarta.validation.Validator jakartaValidator) {
      if (jakartaValidator instanceof InfraValidatorAdapter adapter) {
        return new ValidatorAdapter(adapter, existingBean);
      }
      return new ValidatorAdapter(new InfraValidatorAdapter(jakartaValidator), existingBean);
    }
    return validator;
  }

}
