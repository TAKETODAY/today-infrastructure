/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY <br>
 *         2018-08-04 15:58
 */
@Order(Ordered.HIGHEST_PRECEDENCE - 1)
public class ValuePropertyResolver implements PropertyValueResolver {

    @Override
    public boolean supports(ApplicationContext applicationContext, Field field) {
        return field.isAnnotationPresent(Value.class);
    }

    /**
     * Resolve {@link Value} annotation property.
     */
    @Override
    public PropertyValue resolveProperty(ApplicationContext applicationContext, Field field) {

        final Value annotation = field.getAnnotation(Value.class);
        String expression = annotation.value();

        if (StringUtils.isEmpty(expression)) {
            // use class full name and field name
            expression = new StringBuilder(Constant.PLACE_HOLDER_PREFIX) //
                    .append(field.getDeclaringClass().getName())//
                    .append(Constant.PACKAGE_SEPARATOR)//
                    .append(field.getName())//
                    .append(Constant.PLACE_HOLDER_SUFFIX).toString();
        }
        try {

            final Object resolved = ContextUtils.resolveValue(expression, field.getType());
            if (resolved == null) {
                return required(field, annotation, expression);
            }
            return new PropertyValue(resolved, field);
        }
        catch (ConfigurationException e) {
            return required(field, annotation, expression);
        }
    }

    private final PropertyValue required(Field field, final Value annotation, String expression) {
        if (annotation.required()) {
            throw new ConfigurationException("Can't resolve field: [" + field + "] -> [" + expression + "].");
        }
        return null;
    }

}
