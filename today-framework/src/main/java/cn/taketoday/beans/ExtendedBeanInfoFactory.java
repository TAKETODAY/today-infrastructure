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

package cn.taketoday.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Method;

import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Nullable;

/**
 * {@link BeanInfoFactory} implementation that evaluates whether bean classes have
 * "non-standard" JavaBeans setter methods and are thus candidates for introspection
 * by Framework's (package-visible) {@code ExtendedBeanInfo} implementation.
 *
 * <p>Ordered at {@link Ordered#LOWEST_PRECEDENCE} to allow other user-defined
 * {@link BeanInfoFactory} types to take precedence.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanInfoFactory
 * @see CachedIntrospectionResults
 * @since 4.0 2022/2/23 11:27
 */
public class ExtendedBeanInfoFactory implements BeanInfoFactory, Ordered {

  /**
   * Return an {@link ExtendedBeanInfo} for the given bean class, if applicable.
   */
  @Override
  @Nullable
  public BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
    return supports(beanClass) ? new ExtendedBeanInfo(Introspector.getBeanInfo(beanClass)) : null;
  }

  /**
   * Return whether the given bean class declares or inherits any non-void
   * returning bean property or indexed property setter methods.
   */
  private boolean supports(Class<?> beanClass) {
    for (Method method : beanClass.getMethods()) {
      if (ExtendedBeanInfo.isCandidateWriteMethod(method)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

}
