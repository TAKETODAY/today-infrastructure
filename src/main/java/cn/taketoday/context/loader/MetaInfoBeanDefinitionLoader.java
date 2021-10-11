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

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.lang.Constant;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY 2021/10/7 22:31
 * @since 4.0
 */
public class MetaInfoBeanDefinitionLoader implements BeanDefinitionLoader {
  private static final Logger log = LoggerFactory.getLogger(MetaInfoBeanDefinitionLoader.class);

  /**
   * Resolve bean from META-INF/beans
   *
   * @see Constant#META_INFO_beans
   * @since 2.1.6
   */
  @Override
  public void loadBeanDefinitions(ConfigurableApplicationContext context, BeanDefinitionRegistry registry) {

  }

}
