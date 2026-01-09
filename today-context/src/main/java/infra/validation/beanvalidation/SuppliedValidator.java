/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.validation.beanvalidation;

import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

import infra.lang.Assert;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/23 21:03
 */
public class SuppliedValidator implements Validator {
  private final Supplier<Validator> validatorSupplier;

  @Nullable
  private volatile Validator validator;

  public SuppliedValidator(Supplier<Validator> validatorSupplier) {
    Assert.notNull(validatorSupplier, "validatorSupplier is required");
    this.validatorSupplier = validatorSupplier;
  }

  @Override
  public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
    return getValidator().validate(object, groups);
  }

  @Override
  public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
    return getValidator().validateProperty(object, propertyName, groups);
  }

  @Override
  public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
    return getValidator().validateValue(beanType, propertyName, value, groups);
  }

  @Override
  public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
    return getValidator().getConstraintsForClass(clazz);
  }

  @Override
  public <T> T unwrap(Class<T> type) {
    //allow unwrapping into public super types; intentionally not exposing the
    //fact that ExecutableValidator is implemented by this class as well as this
    //might change
    if (type.isInstance(this)) {
      return type.cast(this);
    }

    return getValidator().unwrap(type);
  }

  @Override
  public ExecutableValidator forExecutables() {
    return getValidator().forExecutables();
  }

  public Validator getValidator() {
    Validator validator = this.validator;
    if (validator == null) {
      synchronized(validatorSupplier) {
        validator = this.validator;
        if (validator == null) {
          validator = validatorSupplier.get();
          this.validator = validator;
        }
      }
    }
    return validator;
  }

}
