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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Condition;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.utils.ClassUtils;

/**
 * {@link Conditional} that only matches when the specified classes are on the
 * classpath.
 * 
 * @author TODAY <br>
 *         2019-06-18 15:00
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Conditional(OnClassCondition.class)
public @interface ConditionalOnClass {

    /**
     * The classes that must be present
     * 
     * @return the classes that must be present
     */
    String[] value() default {};

}

class OnClassCondition implements Condition {

    @Override
    public boolean matches(ApplicationContext applicationContext, AnnotatedElement annotatedElement) {

        final ConditionalOnClass conditionalOnClass = annotatedElement.getAnnotation(ConditionalOnClass.class);
        if (conditionalOnClass != null) {
            for (String name : conditionalOnClass.value()) {
                if (!ClassUtils.isPresent(name)) {
                    return false;
                }
            }
        }
        final ConditionalOnMissingClass onMissingClass = annotatedElement.getAnnotation(ConditionalOnMissingClass.class);
        if (onMissingClass != null) {
            for (String name : onMissingClass.value()) {
                if (ClassUtils.isPresent(name)) {
                    return false;
                }
            }
        }
        
        return true;
    }

}
