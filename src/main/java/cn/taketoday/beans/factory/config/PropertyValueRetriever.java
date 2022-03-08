/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.factory.config;

import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;

/**
 * interface for property value lazy loading
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/27 20:46</a>
 * @since 4.0
 */
public interface PropertyValueRetriever {

  /**
   * It shows that the value is not set
   */
  Object DO_NOT_SET = new Object();

  /**
   * retrieve property-path corresponding property-value
   *
   * @param propertyPath property name
   * @param binder BeanWrapper
   * @param beanFactory own bean factory
   * @return property-value maybe {@link #DO_NOT_SET} indicates that do not set property
   * @throws NoSuchPropertyException If no such property
   */
  Object retrieve(String propertyPath, BeanWrapper binder, AutowireCapableBeanFactory beanFactory);

}
