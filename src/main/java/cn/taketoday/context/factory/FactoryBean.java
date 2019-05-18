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
package cn.taketoday.context.factory;

/**
 * @author Today <br>
 * 
 *         2018-08-03 17:38
 */
public interface FactoryBean<T> {

    /**
     * get bean instance
     * 
     * @return bean instance
     */
    T getBean();

    /**
     * get bean name
     * 
     * @return bean name
     */
    default String getBeanName() {
        return null;
    }

    /**
     * Get bean class
     * 
     * @return bean class
     * @since 2.1.2
     */
    Class<T> getBeanClass();

}
