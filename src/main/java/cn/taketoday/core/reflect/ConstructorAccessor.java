/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.reflect;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;

import cn.taketoday.core.Assert;
import cn.taketoday.util.ReflectionUtils;

/**
 * Constructor accessor
 *
 * @author TODAY 2020.08.26
 */
public abstract class ConstructorAccessor implements Accessor {

  /**
   * Invoke default {@link java.lang.reflect.Constructor}
   *
   * @return returns Object
   */
  public Object newInstance() {
    return newInstance(null);
  }

  /**
   * Invoke {@link java.lang.reflect.Constructor} with given args
   *
   * @return returns Object
   */
  public abstract Object newInstance(Object[] args);

  // static factory

  /**
   * @param target
   */
  public static ConstructorAccessor fromClass(final Class<?> target) {
    Assert.notNull(target, "target class must not be null");

    if (target.isArray()) {
      Class<?> componentType = target.getComponentType();
      return new ArrayConstructor(componentType);
    }
    else if (Collection.class.isAssignableFrom(target)) {
      return new CollectionConstructor(target);
    }
    else if (Map.class.isAssignableFrom(target)) {
      return new MapConstructor(target);
    }

    try {
      final Constructor<?> constructor = target.getDeclaredConstructor();
      return fromConstructor(constructor);
    }
    catch (NoSuchMethodException e) {
      throw new ReflectionException("Target class: '" + target + "‘ has no default constructor");
    }
  }

  /**
   * Fast call bean's {@link java.lang.reflect.Constructor Constructor}
   */
  public static ConstructorAccessor fromConstructor(final Constructor<?> constructor) {
    return new ConstructorAccessorGenerator(constructor).create();
  }

  public static ConstructorAccessor fromReflective(Constructor<?> constructor) {
    Assert.notNull(constructor, "constructor must not be null");
    ReflectionUtils.makeAccessible(constructor);
    return new ReflectiveConstructorAccessor(constructor);
  }

}
