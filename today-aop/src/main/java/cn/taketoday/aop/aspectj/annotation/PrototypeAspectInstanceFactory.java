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

package cn.taketoday.aop.aspectj.annotation;

import java.io.Serializable;

import cn.taketoday.aop.aspectj.AspectInstanceFactory;
import cn.taketoday.beans.factory.BeanFactory;

/**
 * {@link AspectInstanceFactory} backed by a
 * {@link BeanFactory}-provided prototype, enforcing prototype semantics.
 *
 * <p>Note that this may instantiate multiple times, which probably won't give the
 * semantics you expect. Use a {@link LazySingletonAspectInstanceFactoryDecorator}
 * to wrap this to ensure only one new aspect comes back.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see cn.taketoday.beans.factory.BeanFactory
 * @see LazySingletonAspectInstanceFactoryDecorator
 * @since 4.0
 */
@SuppressWarnings("serial")
public class PrototypeAspectInstanceFactory extends BeanFactoryAspectInstanceFactory implements Serializable {

  /**
   * Create a PrototypeAspectInstanceFactory. AspectJ will be called to
   * introspect to create AJType metadata using the type returned for the
   * given bean name from the BeanFactory.
   *
   * @param beanFactory the BeanFactory to obtain instance(s) from
   * @param name the name of the bean
   */
  public PrototypeAspectInstanceFactory(BeanFactory beanFactory, String name) {
    super(beanFactory, name);
    if (!beanFactory.isPrototype(name)) {
      throw new IllegalArgumentException(
              "Cannot use PrototypeAspectInstanceFactory with bean named '" + name + "': not a prototype");
    }
  }

}
