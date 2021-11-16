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

import java.util.function.Supplier;

import cn.taketoday.beans.PropertyException;
import cn.taketoday.beans.dependency.DependencySetter;
import cn.taketoday.beans.dependency.ObjectSupplierDependencySetter;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.ResolvableType;

/**
 * for {@link ObjectSupplier} PropertyValueResolver
 *
 * @author TODAY 2021/3/6 12:10
 * @see Supplier
 * @see ObjectSupplier
 * @since 3.0
 */
public class ObjectSupplierPropertyResolver
        extends AbstractPropertyValueResolver implements PropertyValueResolver {

  @Override
  protected boolean supportsProperty(PropertyResolvingContext context, BeanProperty property) {
    return (property.getType() == Supplier.class
            || property.getType() == ObjectSupplier.class)
            && AutowiredPropertyResolver.isInjectable(property);
  }

  @Override
  protected DependencySetter resolveInternal(PropertyResolvingContext context, BeanProperty property) {
    ResolvableType resolvableType = ResolvableType.fromField(property.getField());
    if (resolvableType.hasGenerics()) {
      ResolvableType generic = resolvableType.getGeneric(0);
      return new ObjectSupplierDependencySetter(property, generic);
    }
    // Usage error
    throw new PropertyException("Unsupported '" + property + "' In -> " + property.getDeclaringClass());
  }

}
