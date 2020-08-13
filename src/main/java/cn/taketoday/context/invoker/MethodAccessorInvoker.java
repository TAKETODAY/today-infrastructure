/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

package cn.taketoday.context.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cn.taketoday.context.reflect.ReflectionException;
import cn.taketoday.context.utils.ReflectionUtils;
import sun.reflect.MethodAccessor;

/**
 * @author TODAY <br>
 * 2020-08-13 18:46
 */
@SuppressWarnings("restriction")
public class MethodAccessorInvoker implements Invoker {

  private final MethodAccessor methodAccessor;

  public MethodAccessorInvoker(MethodAccessor methodAccessor) {
    this.methodAccessor = methodAccessor;
  }

  @Override
  public Object invoke(Object obj, Object[] args) {
    try {
      return methodAccessor.invoke(obj, args);
    }
    catch (InvocationTargetException e) {
      throw new ReflectionException(e.getCause());
    }
  }

  /**
   * Create a {@link MethodAccessorInvoker}
   *
   * @param method
   *     Target method to invoke
   *
   * @return {@link MethodAccessorInvoker} sub object
   */
  public static MethodAccessorInvoker create(Method method) {
    return new MethodAccessorInvoker(ReflectionUtils.newMethodAccessor(method));
  }

  /**
   * Create a {@link MethodAccessorInvoker}
   *
   * @param beanClass
   *     Bean Class
   * @param name
   *     Target method to invoke
   * @param parameters
   *     Target parameters classes
   *
   * @return {@link MethodAccessorInvoker} sub object
   *
   * @throws NoSuchMethodException
   *     Thrown when a particular method cannot be found.
   */
  public static MethodAccessorInvoker create(final Class<?> beanClass,
                                             final String name, final Class<?>... parameters) throws NoSuchMethodException {
    final Method targetMethod = beanClass.getDeclaredMethod(name, parameters);
    return create(targetMethod);
  }

}
