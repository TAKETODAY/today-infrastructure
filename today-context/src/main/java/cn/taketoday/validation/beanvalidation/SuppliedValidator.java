/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
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
