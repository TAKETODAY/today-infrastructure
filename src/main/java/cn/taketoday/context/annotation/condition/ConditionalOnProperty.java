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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.annotation.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;

import cn.taketoday.context.Condition;
import cn.taketoday.context.Constant;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * {@link Conditional} that checks if the specified properties have a specific
 * value. By default the properties must be present in the {@link Environment}
 *
 * @author TODAY <br>
 *         2019-06-18 15:06
 */
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnPropertyCondition.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalOnProperty {

    /**
     * property names
     * 
     * @return the names
     */
    String[] value() default {};

    /**
     * A prefix that should be applied to each property. The prefix automatically
     * ends with a dot if not specified. A valid prefix is defined by one or more
     * words separated with dots (e.g. {@code "acme.system.feature"}).
     * 
     * @return the prefix
     */
    String prefix() default Constant.BLANK;

}

class OnPropertyCondition implements Condition {

    @Override
    public boolean matches(AnnotatedElement annotatedElement) {

        final ConditionalOnProperty conditionalOnProperty = annotatedElement.getAnnotation(ConditionalOnProperty.class);
        final String prefix = conditionalOnProperty.prefix();

        final Environment environment = ContextUtils.getApplicationContext().getEnvironment();
        if (StringUtils.isEmpty(prefix)) for (final String key : conditionalOnProperty.value()) {
            if (environment.getProperty(key) == null) {
                return false;
            }
        }
        else {
            for (final String key : conditionalOnProperty.value()) {
                if (environment.getProperty(prefix + key) == null) {
                    return false;
                }
            }
        }
        return true;
    }

}
