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

package cn.taketoday.beans.factory.support;

import cn.taketoday.beans.factory.config.BeanPostProcessor;

/**
 * Post-processor callback interface for <i>merged</i> bean definitions at runtime.
 * {@link BeanPostProcessor} implementations may implement this sub-interface in order
 * to post-process the merged bean definition (a processed copy of the original bean
 * definition) that the Framework {@code BeanFactory} uses to create a bean instance.
 *
 * <p>The {@link #postProcessMergedBeanDefinition} method may for example introspect
 * the bean definition in order to prepare some cached metadata before post-processing
 * actual instances of a bean. It is also allowed to modify the bean definition but
 * <i>only</i> for definition properties which are actually intended for concurrent
 * modification. Essentially, this only applies to operations defined on the
 * {@link RootBeanDefinition} itself but not to the properties of its base classes.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/3 17:56
 */
public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

  /**
   * Post-process the given merged bean definition for the specified bean.
   *
   * @param beanDefinition the merged bean definition for the bean
   * @param bean the actual type of the managed bean instance
   * @param beanName the name of the bean
   * @see AbstractAutowireCapableBeanFactory#applyBeanDefinitionPostProcessors
   */
  void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Object bean, String beanName);

  /**
   * A notification that the bean definition for the specified name has been reset,
   * and that this post-processor should clear any metadata for the affected bean.
   * <p>The default implementation is empty.
   *
   * @param beanName the name of the bean
   * @see StandardBeanFactory#resetBeanDefinition
   */
  default void resetBeanDefinition(String beanName) { }

}
