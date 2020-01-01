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
package cn.taketoday.context.loader;

import java.lang.reflect.Parameter;

import cn.taketoday.context.factory.BeanFactory;

/**
 * Resolve method parameter object
 * 
 * @author TODAY <br>
 *         2019-10-14 14:11
 */
@FunctionalInterface
public interface ExecutableParameterResolver {

    /**
     * If this {@link ExecutableParameterResolver} supports target {@link Parameter}
     * 
     * @param parameter
     *            Target method {@link Parameter}
     * @return If supports target {@link Parameter}
     */
    default boolean supports(Parameter parameter) {
        return true;
    }

    /**
     * Resolve method parameter object
     * 
     * @param parameter
     *            Target method {@link Parameter}
     * @param beanFactory
     *            {@link BeanFactory}
     * @return parameter object
     */
    Object resolve(Parameter parameter, BeanFactory beanFactory);

    @FunctionalInterface
    public interface SupportsFunction {

        boolean supports(Parameter parameter);
    }

}
