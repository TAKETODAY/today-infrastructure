/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * Bean property applier
 *
 * @author TODAY <br>
 * 2018-06-23 11:28:01
 */
public interface PropertyValue {

  /**
   * It shows that the value is not set
   */
  Object DO_NOT_SET = new Object();

  /**
   * Get Property name
   *
   * @return Property name
   */
  String getName();

  /**
   * set value to property
   * <p>
   * If property value is {@link #DO_NOT_SET} will not set value
   * </p>
   *
   * @param bean
   *         property's bean
   * @param beanFactory
   *         current AbstractBeanFactory
   */
  void applyValue(Object bean, AbstractBeanFactory beanFactory);
}
