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

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.lang.Nullable;

/**
 * process dependency injection
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/19 21:35</a>
 * @since 4.0
 */
public interface DependenciesBeanPostProcessor extends BeanPostProcessor {

  /**
   * Post-process the given property values before the factory applies them
   * to the given bean.
   * <p>The default implementation returns the given {@code pvs} as-is.
   *
   * @param pvs the property values that the factory is about to apply (never {@code null})
   * @param bean the bean instance created, but whose properties have not yet been set
   * @param beanName the name of the bean
   * @throws BeansException in case of errors
   * @see BeanDefinition#isEnableDependencyInjection()
   */
  void processDependencies(@Nullable PropertyValues pvs, Object bean, String beanName);
}
