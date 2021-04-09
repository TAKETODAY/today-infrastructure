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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.factory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import cn.taketoday.context.cglib.proxy.Enhancer;
import cn.taketoday.context.cglib.proxy.MethodInterceptor;

/**
 * The helper class achieve the effect of the prototype
 *
 * @author TODAY <br>
 * 2019-09-03 21:20
 */
public final class Prototypes {

  private final BeanDefinition def;
  private final ConfigurableBeanFactory factory;

  private Prototypes(ConfigurableBeanFactory factory, BeanDefinition def) {
    this.def = def;
    this.factory = factory;
  }

  private Object handle(final Method method, final Object[] a) throws Throwable {
    final Object bean = factory.getBean(def);
    try {
      return method.invoke(bean, a);
    }
    catch (InvocationTargetException e) {
      throw e.getTargetException();
    }
    finally {
      if (factory.isFullLifecycle()) {
        factory.destroyBean(bean, def); // destroyBean after every call
      }
    }
  }

  /**
   * if a property is prototype bean this bean-factory
   * will inject a proxy instance to get prototype
   * instance from every single method call.
   */
  public static Object newProxyInstance(Class<?> refType, BeanDefinition def, ConfigurableBeanFactory factory) {
    return newProxyInstance(refType, def, factory, false);
  }

  /**
   * if a property is prototype bean this bean-factory
   * will inject a proxy instance to get prototype
   * instance from every single method call.
   *
   * @param refType
   *         Reference bean class
   * @param def
   *         Target {@link BeanDefinition}
   * @param factory
   *         {@link AbstractBeanFactory}
   * @param proxyTargetClass
   *         If true use cglib
   *
   * @return Target prototype object
   */
  public static Object newProxyInstance(Class<?> refType,
                                        BeanDefinition def,
                                        ConfigurableBeanFactory factory,
                                        boolean proxyTargetClass) //
  {
    final Prototypes handler = new Prototypes(factory, def);
    if (!proxyTargetClass && refType.isInterface()) { // Use Jdk Proxy
      return Proxy.newProxyInstance(
              refType.getClassLoader(),
              def.getBeanClass().getInterfaces(),
              (final Object p, final Method m, final Object[] a) -> handler.handle(m, a)
      );
    }
    return new Enhancer()
            .setUseCache(true)
            .setSuperclass(refType)
            .setInterfaces(refType.getInterfaces())
            .setClassLoader(refType.getClassLoader())
            .setCallback((MethodInterceptor) (obj, m, a, proxy) -> handler.handle(m, a))
            .create();
  }

}
