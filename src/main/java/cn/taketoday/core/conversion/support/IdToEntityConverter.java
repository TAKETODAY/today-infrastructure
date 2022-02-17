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

package cn.taketoday.core.conversion.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConditionalGenericConverter;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Converts an entity identifier to a entity reference by calling a static finder method
 * on the target entity type.
 *
 * <p>For this converter to match, the finder method must be static, have the signature
 * {@code find[EntityName]([IdType])}, and return an instance of the desired entity type.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class IdToEntityConverter implements ConditionalGenericConverter {

  private final ConversionService conversionService;

  public IdToEntityConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    Method finder = getFinder(targetType.getType());
    return (finder != null &&
            this.conversionService.canConvert(sourceType, TypeDescriptor.valueOf(finder.getParameterTypes()[0])));
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (source == null) {
      return null;
    }
    Method finder = getFinder(targetType.getType());
    Assert.state(finder != null, "No finder method");
    Object id = this.conversionService.convert(
            source, sourceType, TypeDescriptor.valueOf(finder.getParameterTypes()[0]));
    return ReflectionUtils.invokeMethod(finder, source, id);
  }

  @Nullable
  private Method getFinder(Class<?> entityClass) {
    String finderMethod = "find" + getEntityName(entityClass);
    Method[] methods;
    boolean localOnlyFiltered;
    try {
      methods = entityClass.getDeclaredMethods();
      localOnlyFiltered = true;
    }
    catch (SecurityException ex) {
      // Not allowed to access non-public methods...
      // Fallback: check locally declared public methods only.
      methods = entityClass.getMethods();
      localOnlyFiltered = false;
    }
    for (Method method : methods) {
      if (Modifier.isStatic(method.getModifiers()) && method.getName().equals(finderMethod) &&
              method.getParameterCount() == 1 && method.getReturnType().equals(entityClass) &&
              (localOnlyFiltered || method.getDeclaringClass().equals(entityClass))) {
        return method;
      }
    }
    return null;
  }

  private String getEntityName(Class<?> entityClass) {
    String shortName = ClassUtils.getShortName(entityClass);
    int lastDot = shortName.lastIndexOf('.');
    if (lastDot != -1) {
      return shortName.substring(lastDot + 1);
    }
    else {
      return shortName;
    }
  }

}
