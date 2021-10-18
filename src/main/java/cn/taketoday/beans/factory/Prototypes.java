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

import cn.taketoday.aop.proxy.ProxyFactory;
import cn.taketoday.aop.target.PrototypeTargetSource;

/**
 * The helper class achieve the effect of the prototype
 *
 * @author TODAY <br>
 * 2019-09-03 21:20
 */
public final class Prototypes {

  /**
   * if a property is prototype bean this bean-factory
   * will inject a proxy instance to get prototype
   * instance from every single method call.
   *
   * @since 4.0
   */
  public static Object newProxyInstance(BeanDefinition def, BeanFactory factory) {
    return newProxyInstance(def.getBeanClass(), def, factory, false);
  }

  /**
   * if a property is prototype bean this bean-factory
   * will inject a proxy instance to get prototype
   * instance from every single method call.
   */
  public static Object newProxyInstance(Class<?> refType, BeanDefinition def, BeanFactory factory) {
    return newProxyInstance(refType, def, factory, false);
  }

  /**
   * if a property is prototype bean this bean-factory
   * will inject a proxy instance to get prototype
   * instance from every single method call.
   *
   * @param refType Reference bean class
   * @param def Target {@link BeanDefinition}
   * @param factory {@link AbstractBeanFactory}
   * @param proxyTargetClass If true use cglib
   * @return Target prototype object
   */
  public static Object newProxyInstance(
          Class<?> refType, BeanDefinition def, BeanFactory factory, boolean proxyTargetClass) {
    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.setProxyTargetClass(proxyTargetClass);

    PrototypeTargetSource prototypeTargetSource = new PrototypeTargetSource();
    prototypeTargetSource.setTargetBeanDefinition(def);
    prototypeTargetSource.setBeanFactory(factory);
    proxyFactory.setTargetSource(prototypeTargetSource);
    return proxyFactory.getProxy(refType.getClassLoader());
  }

}
