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
package cn.taketoday.context.factory;

import cn.taketoday.context.exception.BeanDefinitionStoreException;

/**
 * @author TODAY <br>
 *         2018-07-18 1:01:19
 */
public interface BeanPostProcessor {

    /**
     * Before property set
     * 
     * @param bean
     *            Bean instance
     * @param def
     *            Bean definition
     * @return Bean instance
     * @throws Exception
     *             In case of errors
     */
    default Object postProcessBeforeInitialization(Object bean, BeanDefinition def) throws Exception {
        return bean;
    }

    /**
     * After property set
     * 
     * @param bean
     *            Bean instance
     * @param def
     *            {@link BeanDefinitionStoreException}
     * @return Bean instance
     * @throws Exception
     *             In case of errors
     */
    default Object postProcessAfterInitialization(Object bean, BeanDefinition def) throws Exception {
        return bean;
    }
}
