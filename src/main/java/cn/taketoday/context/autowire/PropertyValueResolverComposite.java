/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.autowire;

import java.util.ArrayList;
import java.util.Collections;

import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * PropertyValueResolver Composite
 * <p>
 * supports order
 *
 * @author TODAY 2021/10/3 22:13
 * @since 4.0
 */
public class PropertyValueResolverComposite implements PropertyValueResolver, ArraySizeTrimmer {

  /**
   * @since 3.0 Resolve {@link PropertySetter}
   */
  private final ArrayList<PropertyValueResolver> propertyResolvers = new ArrayList<>(4);

  @Nullable
  @Override
  public PropertySetter resolveProperty(PropertyResolvingContext context, BeanProperty property) {
    for (PropertyValueResolver propertyValueResolver : propertyResolvers) {
      PropertySetter propertySetter = propertyValueResolver.resolveProperty(context, property);
      if (propertySetter != null) {
        return propertySetter;
      }
    }
    return null;
  }

  /**
   * @since 4.0
   */
  public ArrayList<PropertyValueResolver> getResolvers() {
    return propertyResolvers;
  }

  /**
   * @since 3.0
   */
  public void setResolvers(PropertyValueResolver... resolvers) {
    Assert.notNull(resolvers, "PropertyValueResolver must not be null");

    propertyResolvers.clear();
    AnnotationAwareOrderComparator.sort(resolvers);
    Collections.addAll(propertyResolvers, resolvers);
  }

  /**
   * Add {@link PropertyValueResolver} to {@link #propertyResolvers}
   *
   * @param resolvers {@link PropertyValueResolver} object
   * @since 3.0
   */
  public void addResolvers(PropertyValueResolver... resolvers) {
    if (ObjectUtils.isNotEmpty(resolvers)) {
      Collections.addAll(propertyResolvers, resolvers);
      AnnotationAwareOrderComparator.sort(propertyResolvers);
    }
  }

  /**
   * @since 4.0
   */
  public void clear() {
    propertyResolvers.clear();
  }

  @Override
  public void trimToSize() {
    propertyResolvers.trimToSize();
  }
}
