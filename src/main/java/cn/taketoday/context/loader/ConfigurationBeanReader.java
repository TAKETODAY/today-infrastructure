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

import java.util.Map;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.lang.Configuration;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY 2021/10/16 23:17
 * @see cn.taketoday.lang.Configuration
 * @since 4.0
 */
public class ConfigurationBeanReader implements BeanFactoryPostProcessor {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationBeanReader.class);
  final BeanDefinitionReader beanDefinitionReader;

  public ConfigurationBeanReader(BeanDefinitionReader beanDefinitionReader) {
    this.beanDefinitionReader = beanDefinitionReader;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    loadConfigurationBeans();
  }

  /**
   * Resolve bean from a class which annotated with @{@link Configuration}
   */
  public void loadConfigurationBeans() {
    log.debug("Loading Configuration Beans");

    BeanDefinitionRegistry registry = beanDefinitionReader.obtainRegistry();
    for (Map.Entry<String, BeanDefinition> entry : registry.getBeanDefinitions().entrySet()) {
      if (entry.getValue().isAnnotationPresent(Configuration.class)) {
        // @Configuration bean
        beanDefinitionReader.loadConfigurationBeans(entry.getValue());
      }
    }
  }

}
