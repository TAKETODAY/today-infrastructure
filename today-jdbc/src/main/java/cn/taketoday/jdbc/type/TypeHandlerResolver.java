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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * BeanProperty {@link TypeHandler} resolver
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/30 23:32
 */
public interface TypeHandlerResolver {

  /**
   * BeanProperty {@link TypeHandler} resolver
   *
   * @param beanProperty target property
   */
  @Nullable
  TypeHandler<?> resolve(BeanProperty beanProperty);

  /**
   * returns a new resolving chain
   *
   * @param next next resolver
   * @return returns a new resolving chain
   */
  default TypeHandlerResolver and(TypeHandlerResolver next) {
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

  /**
   * Use {@link MappedTypeHandler} to resolve {@link TypeHandler}
   *
   * @return Annotation based {@link TypeHandlerResolver}
   * @see MergedAnnotation#getClass(String)
   */
  static TypeHandlerResolver forMappedTypeHandlerAnnotation() {
    return forAnnotation(MappedTypeHandler.class);
  }

  /**
   * Use input {@code annotationType} to resolve {@link TypeHandler}
   *
   * @param annotationType Annotation type
   * @return Annotation based {@link TypeHandlerResolver}
   * @see MergedAnnotation#getClass(String)
   */
  static TypeHandlerResolver forAnnotation(Class<? extends Annotation> annotationType) {
    return forAnnotation(annotationType, MergedAnnotation.VALUE);
  }

  /**
   * Use input {@code annotationType} and {@code attributeName} to resolve {@link TypeHandler}
   *
   * @param annotationType Annotation type
   * @param attributeName the attribute name
   * @return Annotation based {@link TypeHandlerResolver}
   * @see MergedAnnotation#getClass(String)
   */
  static TypeHandlerResolver forAnnotation(Class<? extends Annotation> annotationType, String attributeName) {
    Assert.notNull(attributeName, "attributeName is required");
    Assert.notNull(annotationType, "annotationType is required");

    return (BeanProperty property) -> {
      var mappedTypeHandler = MergedAnnotations.from(
              property, property.getAnnotations()).get(annotationType);
      if (mappedTypeHandler.isPresent()) {
        // user defined TypeHandler
        Class<? extends TypeHandler<?>> typeHandlerClass = mappedTypeHandler.getClass(attributeName);
        Constructor<? extends TypeHandler<?>> constructor = BeanUtils.getConstructor(typeHandlerClass);
        if (constructor == null) {
          throw new IllegalStateException("No suitable constructor in " + typeHandlerClass);
        }

        if (constructor.getParameterCount() != 0) {
          Object[] args = new Object[constructor.getParameterCount()];
          Class<?>[] parameterTypes = constructor.getParameterTypes();
          int i = 0;
          for (Class<?> parameterType : parameterTypes) {
            args[i++] = resolveArg(property, parameterType);
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
      return beanProperty.getType();
    }
    if (parameterType == BeanProperty.class) {
      return beanProperty;
    }
    throw new IllegalArgumentException(
            "TypeHandler Constructor parameterType '" + parameterType.getName() + "' currently not supported");
  }

}
