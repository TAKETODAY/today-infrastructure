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

import static cn.taketoday.context.utils.ClassUtils.isAnnotationPresent;

import java.lang.reflect.Field;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.aware.OrderedApplicationContextSupport;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.PropertyValue;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY <br>
 *         2018-08-04 15:58
 */
public class ValuePropertyResolver extends OrderedApplicationContextSupport implements PropertyValueResolver {

    public ValuePropertyResolver(ApplicationContext context) {
        this(context, Ordered.HIGHEST_PRECEDENCE - 1);
    }

    public ValuePropertyResolver(ApplicationContext context, int order) {
        super(order);
        setApplicationContext(context);
    }

    @Override
    public boolean supports(final Field field) {
        return isAnnotationPresent(field, Value.class)
               || isAnnotationPresent(field, Env.class);
    }

    /**
     * Resolve {@link Value} and {@link Env} annotation property.
     */
    @Override
    public PropertyValue resolveProperty(final Field field) {

        String expression;
        final boolean required;

        final Value value = ClassUtils.getAnnotation(Value.class, field);
        if (value != null) {
            expression = value.value();
            required = value.required();
        }
        else {
            final Env env = ClassUtils.getAnnotation(Env.class, field);
            required = env.required();
            expression = env.value();

            if (StringUtils.isNotEmpty(expression)) {
                expression = new StringBuilder(expression.length() + 3)//
                        .append(Constant.PLACE_HOLDER_PREFIX)//
                        .append(expression)//
                        .append(Constant.PLACE_HOLDER_SUFFIX).toString();
            }
        }

        if (StringUtils.isEmpty(expression)) {
            // use class full name and field name
            expression = new StringBuilder(Constant.PLACE_HOLDER_PREFIX) //
                    .append(field.getDeclaringClass().getName())//
                    .append(Constant.PACKAGE_SEPARATOR)//
                    .append(field.getName())//
                    .append(Constant.PLACE_HOLDER_SUFFIX).toString();
        }
        Object resolved;
        try {
             resolved = ContextUtils.resolveValue(expression, field.getType(), obtainApplicationContext().getEnvironment().getProperties());
        }
        catch (ConfigurationException e) {
            return required(field, required, expression);
        }
        if (resolved == null) {
            return required(field, required, expression);
        }
        return new PropertyValue(resolved, field);
    }

    private PropertyValue required(final Field field, final boolean required, final String expression) {
        if (required) {
            throw new ConfigurationException("Can't resolve field: [" + field + "] -> [" + expression + "].");
        }
        return null;
    }

}
