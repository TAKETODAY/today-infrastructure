/*
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
package cn.taketoday.context.loader;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.lang.Configuration;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;

/**
 * Import classes according to user's Configuration
 *
 * @author TODAY <br>
 * 2019-10-01 20:20
 */
@FunctionalInterface
public interface ImportSelector {

  String[] NO_IMPORTS = Constant.EMPTY_STRING_ARRAY;

  /**
   * Select and return the full names of which class(es) should be imported based
   * on the importing @{@link Configuration} BeanDefinition.
   *
   * @param annotatedMetadata
   *         Target {@link BeanDefinition}
   * @param registry
   *         BeanDefinitionRegistry
   *
   * @return import classes Never be null
   *
   * @see BeanDefinitionRegistry#isBeanNameInUse(String)
   */
  @Nullable
  String[] selectImports(BeanDefinition annotatedMetadata, BeanDefinitionRegistry registry);

}
