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

import cn.taketoday.beans.factory.config.BeanDefinition;

/**
 * Strategy interface for generating bean names for bean definitions.
 * <p>
 * Like Spring's BeanNamePopulator
 * </p>
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface BeanNamePopulator {

  /**
   * Generate a bean name for the given bean definition.
   * <p>
   * populate bean name and its aliases
   * </p>
   *
   * @param definition the bean definition to generate a name for
   * @param registry the bean definition registry that the given definition
   * is supposed to be registered with
   * @return the generated bean name
   */
  String populateName(BeanDefinition definition, BeanDefinitionRegistry registry);

}
