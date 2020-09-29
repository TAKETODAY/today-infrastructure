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
package cn.taketoday.context.aware;

import cn.taketoday.context.factory.InitializingBean;

/**
 * Callback that allows a bean to be aware of the bean {@link ClassLoader class
 * loader}; that is, the class loader used by the present bean factory to load
 * bean classes.
 *
 * <p>
 * This is mainly intended to be implemented by framework classes which have to
 * pick up application classes by name despite themselves potentially being
 * loaded from a shared class loader.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author TODAY <br>
 * 2020-02-21 11:45
 * @see BeanNameAware
 * @see BeanFactoryAware
 * @see InitializingBean
 * @since 2.1.7
 */
public interface BeanClassLoaderAware extends Aware {

  /**
   * Callback that supplies the bean {@link ClassLoader class loader} to a bean
   * instance.
   * <p>
   * Invoked <i>before</i> apply properties
   *
   * @param classLoader
   *         The owning class loader
   */
  void setBeanClassLoader(ClassLoader classLoader);

}
