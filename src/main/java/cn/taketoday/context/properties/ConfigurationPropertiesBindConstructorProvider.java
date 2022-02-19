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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties;

import java.lang.reflect.Constructor;

import cn.taketoday.context.properties.bind.BindConstructorProvider;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link BindConstructorProvider} used when binding
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigurationPropertiesBindConstructorProvider implements BindConstructorProvider {

  static final ConfigurationPropertiesBindConstructorProvider INSTANCE = new ConfigurationPropertiesBindConstructorProvider();

  @Nullable
  @Override
  public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
    return getBindConstructor(bindable.getType().resolve(), isNestedConstructorBinding);
  }

  @Nullable
  Constructor<?> getBindConstructor(@Nullable Class<?> type, boolean isNestedConstructorBinding) {
    if (type == null) {
      return null;
    }
    Constructor<?> constructor = findConstructorBindingAnnotatedConstructor(type);
    if (constructor == null && (isConstructorBindingType(type) || isNestedConstructorBinding)) {
      constructor = deduceBindConstructor(type);
    }
    return constructor;
  }

  @Nullable
  private Constructor<?> findConstructorBindingAnnotatedConstructor(Class<?> type) {
    return findAnnotatedConstructor(type, type.getDeclaredConstructors());
  }

  @Nullable
  private Constructor<?> findAnnotatedConstructor(Class<?> type, Constructor<?>... candidates) {
    Constructor<?> constructor = null;
    for (Constructor<?> candidate : candidates) {
      if (MergedAnnotations.from(candidate).isPresent(ConstructorBinding.class)) {
        Assert.state(candidate.getParameterCount() > 0,
                () -> type.getName() + " declares @ConstructorBinding on a no-args constructor");
        Assert.state(constructor == null,
                () -> type.getName() + " has more than one @ConstructorBinding constructor");
        constructor = candidate;
      }
    }
    return constructor;
  }

  private boolean isConstructorBindingType(Class<?> type) {
    return isImplicitConstructorBindingType(type) || isConstructorBindingAnnotatedType(type);
  }

  private boolean isImplicitConstructorBindingType(Class<?> type) {
    Class<?> superclass = type.getSuperclass();
    return (superclass != null) && "java.lang.Record".equals(superclass.getName());
  }

  private boolean isConstructorBindingAnnotatedType(Class<?> type) {
    return MergedAnnotations.from(type, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY_AND_ENCLOSING_CLASSES)
            .isPresent(ConstructorBinding.class);
  }

  @Nullable
  private Constructor<?> deduceBindConstructor(Class<?> type) {
    Constructor<?>[] constructors = type.getDeclaredConstructors();
    if (constructors.length == 1 && constructors[0].getParameterCount() > 0) {
      return constructors[0];
    }
    return null;
  }

}
