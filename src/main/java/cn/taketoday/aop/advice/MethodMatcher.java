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

import java.lang.reflect.Method;

/**
 * @author TODAY <br>
 *         2019-10-20 22:43
 */
public interface MethodMatcher {

    /**
     * Checking whether the given method matches.
     * 
     * @param method
     *            the candidate method
     * @param targetClass
     *            the target class
     * @return whether or not this method matches on application startup.
     */
    boolean matches(Method method, Class<?> targetClass);

}
