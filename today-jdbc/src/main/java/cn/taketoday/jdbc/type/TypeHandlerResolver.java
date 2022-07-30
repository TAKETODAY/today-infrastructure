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

package cn.taketoday.jdbc.type;

import java.lang.reflect.Constructor;
import java.util.List;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/30 23:32
 */
public interface TypeHandlerResolver {

  @Nullable
  TypeHandler<?> resolve(BeanProperty beanProperty);

  default TypeHandlerResolver add(TypeHandlerResolver next) {
    return beanProperty -> {
      TypeHandler<?> resolved = resolve(beanProperty);
      if (resolved == null) {
        resolved = next.resolve(beanProperty);
      }
      return resolved;
    };
  }

  static TypeHandlerResolver composite(TypeHandlerResolver... resolvers) {
    Assert.notNull(resolvers, "TypeHandlerResolver is required");
    return composite(List.of(resolvers));
  }

  static TypeHandlerResolver composite(List<TypeHandlerResolver> resolvers) {
    Assert.notNull(resolvers, "TypeHandlerResolver is required");
    return beanProperty -> {
      for (TypeHandlerResolver resolver : resolvers) {
        TypeHandler<?> resolved = resolver.resolve(beanProperty);
        if (resolved != null) {
          return resolved;
        }
      }

      return null;
    };
  }

  static TypeHandlerResolver forMappedTypeHandlerAnnotation() {
    return beanProperty -> {
      MappedTypeHandler mappedTypeHandler = beanProperty.getAnnotation(MappedTypeHandler.class);
      if (mappedTypeHandler != null) {
        // user defined TypeHandler
        Class<? extends TypeHandler<?>> typeHandlerClass = mappedTypeHandler.value();
        Constructor<? extends TypeHandler<?>> constructor = BeanUtils.getConstructor(typeHandlerClass);
        if (constructor == null) {
          throw new IllegalStateException("No suitable constructor in " + typeHandlerClass);
        }

        if (constructor.getParameterCount() != 0) {
          Object[] args = new Object[constructor.getParameterCount()];
          Class<?>[] parameterTypes = constructor.getParameterTypes();
          int i = 0;
          for (Class<?> parameterType : parameterTypes) {
            args[i++] = resolveArg(beanProperty, parameterType);
          }
          return BeanUtils.newInstance(constructor, args);
        }
        else {
          return BeanUtils.newInstance(constructor);
        }
      }

      return null;
    };
  }

  private static Object resolveArg(BeanProperty beanProperty, Class<?> parameterType) {
    if (parameterType == Class.class) {
      return parameterType;
    }
    if (parameterType == BeanProperty.class) {
      return beanProperty;
    }
    throw new IllegalArgumentException(
            "TypeHandler Constructor parameterType '" + parameterType.getName() + "' currently not supported");
  }

}
