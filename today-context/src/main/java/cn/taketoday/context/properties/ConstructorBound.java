/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.properties;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.properties.bind.ConstructorBinding;

/**
 * Helper class to programmatically bind configuration properties that use constructor
 * injection.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConstructorBinding
 * @since 4.0
 */
public abstract class ConstructorBound {

  /**
   * Create an immutable {@link ConfigurationProperties} instance for the specified
   * {@code beanName} and {@code beanType} using the specified {@link BeanFactory}.
   *
   * @param beanFactory the bean factory to use
   * @param beanName the name of the bean
   * @param beanType the type of the bean
   * @return an instance from the specified bean
   */
  public static Object from(BeanFactory beanFactory, String beanName, Class<?> beanType) {
    ConfigurationPropertiesBean bean = ConfigurationPropertiesBean.forValueObject(beanType, beanName);
    ConfigurationPropertiesBinder binder = ConfigurationPropertiesBinder.get(beanFactory);
    try {
      return binder.bindOrCreate(bean);
    }
    catch (Exception ex) {
      throw new ConfigurationPropertiesBindException(bean, ex);
    }
  }

}
