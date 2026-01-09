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
