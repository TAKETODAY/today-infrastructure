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

import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.PropertyException;
import cn.taketoday.beans.dependency.DefaultDependencySetter;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.context.DefaultProps;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.util.ClassUtils;

/**
 * @author TODAY 2018-08-04 16:01
 */
public class PropsPropertyResolver implements PropertyValueResolver {

  /**
   * Resolve {@link Props} annotation property.
   */
  @Override
  public DefaultDependencySetter resolveProperty(PropertyResolvingContext context, BeanProperty property) {
    MergedAnnotation<Props> annotation = MergedAnnotations.from(property).get(Props.class);
    if (annotation.isPresent()) {
      Class<?> propertyClass = property.getType();
      if (ClassUtils.isSimpleType(propertyClass)) {
        // not support simple type
        throw new PropertyException(
                "Props usage error, cannot declare it on simple-type property, Use @Value instead");
      }
      DefaultProps props = new DefaultProps(annotation);

      PropsReader propsReader = context.getPropsReader();
      Properties properties = propsReader.readMap(props);

      // feat: Enhance `Props`
      if (!Map.class.isAssignableFrom(propertyClass)) {
        return new DefaultDependencySetter(propsReader.read(props, propertyClass), property);
      }
      return new DefaultDependencySetter(properties, property);
    }
    return null; // next resolver
  }

}
