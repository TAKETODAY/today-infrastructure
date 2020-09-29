/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.aware.OrderedApplicationContextSupport;
import cn.taketoday.context.factory.PropertyValue;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;

/**
 * @author TODAY <br>
 *         2018-08-04 16:01
 */
public class PropsPropertyResolver extends OrderedApplicationContextSupport implements PropertyValueResolver {

  public PropsPropertyResolver(ApplicationContext context) {
    this(context, Ordered.HIGHEST_PRECEDENCE - 2);
  }

  public PropsPropertyResolver(ApplicationContext context, int order) {
    super(order);
    setApplicationContext(context);
  }

  @Override
  public boolean supportsProperty(Field field) {
    return ClassUtils.isAnnotationPresent(field, Props.class);
  }

  /**
   * Resolve {@link Props} annotation property.
   */
  @Override
  public PropertyValue resolveProperty(Field field) {

    Props props = ClassUtils.getAnnotation(Props.class, field);

    Properties properties = //
            ContextUtils.loadProps(props, obtainApplicationContext().getEnvironment().getProperties());

    // feat: Enhance `Props`
    final Class<?> propertyClass = field.getType();
    if (!Map.class.isAssignableFrom(propertyClass)) {

      return new PropertyValue(ContextUtils.resolveProps(props, propertyClass, properties), field);
    }
    return new PropertyValue(properties, field);
  }

}
