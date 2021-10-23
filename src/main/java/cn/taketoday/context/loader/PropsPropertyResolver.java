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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.loader;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.PropertyException;
import cn.taketoday.beans.factory.DefaultPropertySetter;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.context.DefaultProps;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.util.ClassUtils;

/**
 * @author TODAY 2018-08-04 16:01
 */
public class PropsPropertyResolver implements PropertyValueResolver {

  /**
   * Resolve {@link Props} annotation property.
   */
  @Override
  public DefaultPropertySetter resolveProperty(PropertyResolvingContext context, Field field) {
    AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(
            field, Props.class);
    if (attributes != null) {
      Class<?> propertyClass = field.getType();
      if (ClassUtils.isSimpleType(propertyClass)) {
        // not support simple type
        throw new PropertyException(
                "Props usage error, cannot declare it on simple-type property, Use @Value instead");
      }
      DefaultProps props = new DefaultProps(attributes);

      PropsReader propsReader = context.getPropsReader();
      Properties properties = propsReader.readMap(props);

      // feat: Enhance `Props`
      if (!Map.class.isAssignableFrom(propertyClass)) {
        return new DefaultPropertySetter(propsReader.read(props, propertyClass), field);
      }
      return new DefaultPropertySetter(properties, field);
    }
    return null; // next resolver
  }

}
