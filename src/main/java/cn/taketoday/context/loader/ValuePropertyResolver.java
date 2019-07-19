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

import cn.taketoday.context.Constant;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Env;
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
    public boolean supports(Field field) {
        return field.isAnnotationPresent(Value.class) || field.isAnnotationPresent(Env.class);
    }

    /**
     * Resolve {@link Value} and {@link Env} annotation property.
     */
    @Override
    public PropertyValue resolveProperty(Field field) {

        String expression;
        final boolean required;

        final Value value = field.getAnnotation(Value.class);
        if (value != null) {
            expression = value.value();
            required = value.required();
        }
        else {
            final Env env = field.getAnnotation(Env.class);
            required = env.required();
            expression = new StringBuilder()//
                    .append(Constant.PLACE_HOLDER_PREFIX)//
                    .append(env.value())//
                    .append(Constant.PLACE_HOLDER_SUFFIX).toString();
        }

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
                return required(field, required, expression);
            }
            return new PropertyValue(resolved, field);
        }
        catch (ConfigurationException e) {
            return required(field, required, expression);
        }
    }

    private final PropertyValue required(final Field field, final boolean required, final String expression) {
        if (required) {
            throw new ConfigurationException("Can't resolve field: [" + field + "] -> [" + expression + "].");
        }
        return null;
    }

}
