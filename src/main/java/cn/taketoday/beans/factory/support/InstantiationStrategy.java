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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/10 22:06
 */
public class InstantiationStrategy {

  private static final ThreadLocal<Method> currentlyInvokedFactoryMethod = new ThreadLocal<>();

  /**
   * Return the factory method currently being invoked or {@code null} if none.
   * <p>Allows factory method implementations to determine whether the current
   * caller is the container itself as opposed to user code.
   */
  @Nullable
  public static Method getCurrentlyInvokedFactoryMethod() {
    return currentlyInvokedFactoryMethod.get();
  }

  /**
   * Return an instance of the bean with the given name in this factory.
   *
   * @param bd the bean definition
   * @param owner the owning BeanFactory
   * @return a bean instance for this bean definition
   * @throws BeansException if the instantiation attempt failed
   */
  public Object instantiate(BeanDefinition bd, BeanFactory owner) {
    // Don't override the class with CGLIB if no overrides.
    Constructor<?> constructorToUse;
    synchronized(bd.constructorArgumentLock) {
      constructorToUse = (Constructor<?>) bd.executable;
      if (constructorToUse == null) {
        final Class<?> clazz = bd.getBeanClass();
        if (clazz.isInterface()) {
          throw new BeanInstantiationException(clazz, "Specified class is an interface");
        }
        try {
          constructorToUse = clazz.getDeclaredConstructor();
          bd.executable = constructorToUse;
        }
        catch (Throwable ex) {
          throw new BeanInstantiationException(clazz, "No default constructor found", ex);
        }
      }
    }
    return BeanUtils.newInstance(constructorToUse, null);
  }

  /**
   * Return an instance of the bean with the given name in this factory,
   * creating it via the given constructor.
   *
   * @param bd the bean definition
   * @param owner the owning BeanFactory
   * @param ctor the constructor to use
   * @param args the constructor arguments to apply
   * @return a bean instance for this bean definition
   * @throws BeansException if the instantiation attempt failed
   */
  public Object instantiate(
          BeanDefinition bd, BeanFactory owner, final Constructor<?> ctor, Object... args) {
    return BeanUtils.newInstance(ctor, args);
  }

  /**
   * Return an instance of the bean with the given name in this factory,
   * creating it via the given factory method.
   *
   * @param bd the bean definition
   * @param owner the owning BeanFactory
   * @param factoryBean the factory bean instance to call the factory method on,
   * or {@code null} in case of a static factory method
   * @param factoryMethod the factory method to use
   * @param args the factory method arguments to apply
   * @return a bean instance for this bean definition
   * @throws BeansException if the instantiation attempt failed
   */
  public Object instantiate(
          BeanDefinition bd, BeanFactory owner,
          @Nullable Object factoryBean, final Method factoryMethod, Object... args) {

    try {
      ReflectionUtils.makeAccessible(factoryMethod);

      Method priorInvokedFactoryMethod = currentlyInvokedFactoryMethod.get();
      try {
        currentlyInvokedFactoryMethod.set(factoryMethod);
        return factoryMethod.invoke(factoryBean, args);
      }
      finally {
        if (priorInvokedFactoryMethod != null) {
          currentlyInvokedFactoryMethod.set(priorInvokedFactoryMethod);
        }
        else {
          currentlyInvokedFactoryMethod.remove();
        }
      }
    }
    catch (IllegalArgumentException ex) {
      throw new BeanInstantiationException(factoryMethod,
              "Illegal arguments to factory method '" + factoryMethod.getName() + "'; " +
                      "args: " + StringUtils.arrayToCommaDelimitedString(args), ex);
    }
    catch (IllegalAccessException ex) {
      throw new BeanInstantiationException(factoryMethod,
              "Cannot access factory method '" + factoryMethod.getName() + "'; is it public?", ex);
    }
    catch (InvocationTargetException ex) {
      String msg = "Factory method '" + factoryMethod.getName() + "' threw exception";
      if (bd.getFactoryBeanName() != null && owner instanceof ConfigurableBeanFactory &&
              ((ConfigurableBeanFactory) owner).isCurrentlyInCreation(bd.getFactoryBeanName())) {
        msg = "Circular reference involving containing bean '" + bd.getFactoryBeanName() + "' - consider " +
                "declaring the factory method as static for independence from its containing instance. " + msg;
      }
      throw new BeanInstantiationException(factoryMethod, msg, ex.getTargetException());
    }
  }

}
