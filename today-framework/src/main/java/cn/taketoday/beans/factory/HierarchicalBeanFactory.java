/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.beans.factory;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.lang.Nullable;

/**
 * Sub-interface implemented by bean factories that can be part
 * of a hierarchy.
 *
 * <p>The corresponding {@code setParentBeanFactory} method for bean
 * factories that allow setting the parent in a configurable
 * fashion can be found in the ConfigurableBeanFactory interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/9/28 21:21
 * @see ConfigurableBeanFactory#setParentBeanFactory
 * @since 4.0
 */
public interface HierarchicalBeanFactory extends BeanFactory {

  /**
   * Return the parent bean factory, or {@code null} if there is none.
   */
  @Nullable
  BeanFactory getParentBeanFactory();

  /**
   * Return whether the local bean factory contains a bean of the given name,
   * ignoring beans defined in ancestor contexts.
   * <p>This is an alternative to {@code containsBean}, ignoring a bean
   * of the given name from an ancestor bean factory.
   *
   * @param name the name of the bean to query
   * @return whether a bean with the given name is defined in the local factory
   * @see BeanFactory#containsBean
   */
  boolean containsLocalBean(String name);

}
