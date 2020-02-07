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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.aop.advice;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import cn.taketoday.aop.annotation.Advice;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY <br>
 *         2019-10-22 21:39
 */
public class DefaultClassMatcher implements ClassMatcher {

    @Override
    public boolean matches(Object aspect, Class<?> targetClass) {
        final Class<?> aspectClass = aspect.getClass(); // aspect class

//        final Advice[] advices = ClassUtils.getAnnotationArray(aspectClass, Advice.class, AdviceImpl.class);

        return false;
    }

    public static boolean matchClass(final Class<?> targetClass, final Advice[] advices) {

        for (final Advice advice : advices) {
            // target class match start
            for (final Class<?> target : advice.target()) {
                if (target == targetClass) {
                    return true;
                }
            }
            Method[] targetDeclaredMethods = targetClass.getDeclaredMethods(); // target class's methods
            // annotation match start
            for (Class<? extends Annotation> annotation : advice.value()) {
                if (targetClass.isAnnotationPresent(annotation)) {
                    return true;
                }
                for (Method targetMethod : targetDeclaredMethods) {// target class's methods
                    if (targetMethod.isAnnotationPresent(annotation)) {
                        return true;
                    }
                }
            }
            String targetClassName = targetClass.getName();
            for (String regex : advice.pointcut()) { // regex match start
                if (StringUtils.isEmpty(regex)) {
                    continue;
                }

                if (Pattern.matches(regex, targetClassName)) {
                    // class matched
                    return true;
                }
            }
        }
        return false;
    }
}
