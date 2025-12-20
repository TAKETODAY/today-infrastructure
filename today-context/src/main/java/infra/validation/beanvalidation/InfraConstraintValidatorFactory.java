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

package infra.validation.beanvalidation;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.AutowireCapableBeanFactory;
import infra.lang.Assert;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;

/**
 * JSR-303 {@link ConstraintValidatorFactory} implementation that delegates to a
 * Framework BeanFactory for creating autowired {@link ConstraintValidator} instances.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AutowireCapableBeanFactory#createBean(Class)
 * @see infra.context.ApplicationContext#getAutowireCapableBeanFactory()
 * @since 4.0
 */
public class InfraConstraintValidatorFactory implements ConstraintValidatorFactory {

  private final AutowireCapableBeanFactory beanFactory;

  private final @Nullable ConstraintValidatorFactory defaultConstraintValidatorFactory;

  /**
   * Create a new COntextConstraintValidatorFactory for the given BeanFactory.
   *
   * @param beanFactory the target BeanFactory
   */
  public InfraConstraintValidatorFactory(AutowireCapableBeanFactory beanFactory) {
    Assert.notNull(beanFactory, "BeanFactory is required");
    this.beanFactory = beanFactory;
    this.defaultConstraintValidatorFactory = null;
  }

  /**
   * Create a new InfraConstraintValidatorFactory for the given BeanFactory.
   *
   * @param beanFactory the target BeanFactory
   * @param factory the default ConstraintValidatorFactory
   * as exposed by the validation provider (for creating provider-internal validator
   * implementations which might not be publicly accessible in a module path setup)
   * @since 5.0
   */
  public InfraConstraintValidatorFactory(AutowireCapableBeanFactory beanFactory, ConstraintValidatorFactory factory) {
    Assert.notNull(beanFactory, "BeanFactory is required");
    this.beanFactory = beanFactory;
    this.defaultConstraintValidatorFactory = factory;
  }

  @Override
  public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
    if (this.defaultConstraintValidatorFactory != null) {
      // Create provider-internal validator implementations through default ConstraintValidatorFactory.
      String providerModuleName = this.defaultConstraintValidatorFactory.getClass().getModule().getName();
      if (providerModuleName != null && providerModuleName.equals(key.getModule().getName())) {
        return this.defaultConstraintValidatorFactory.getInstance(key);
      }
    }
    return this.beanFactory.createBean(key);
  }

  // Bean Validation 1.1 releaseInstance method
  @Override
  public void releaseInstance(ConstraintValidator<?, ?> instance) {
    this.beanFactory.destroyBean(instance);
  }

}
